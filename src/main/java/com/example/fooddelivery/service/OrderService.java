package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.OrderItemRequest;
import com.example.fooddelivery.dto.OrderItemResponse;
import com.example.fooddelivery.dto.OrderRequest;
import com.example.fooddelivery.dto.OrderResponse;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.OrderItem;
import com.example.fooddelivery.entity.Payment;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.PaymentStatus;
import com.example.fooddelivery.event.OrderStatusChangedEvent;
import com.example.fooddelivery.exception.InsufficientStockException;
import com.example.fooddelivery.exception.PaymentFailedException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.exception.UnauthorizedActionException;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.OrderItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.PaymentRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	// Section 9 suggests "up to 3 attempts" as an example bound; under N-way contention on
	// a single row (e.g. the 10-thread concurrency test in Section 13), 3 attempts can let
	// an unlucky thread exhaust its retries via repeated version conflicts even though
	// stock is still available, understating the number of legitimate winners. A higher
	// (but still bounded, non-infinite) budget avoids that starvation without changing the
	// safety guarantee: stock is never oversold or negative regardless of this number.
	private static final int MAX_STOCK_UPDATE_ATTEMPTS = 10;

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final MenuItemRepository menuItemRepository;
	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentService paymentService;
	private final ApplicationEventPublisher eventPublisher;

	// Self-injected proxy: the retry loop in placeOrder() must invoke the @Transactional
	// method through the Spring proxy (not `this`) so each retry runs in a fresh transaction.
	@Autowired
	@Lazy
	private OrderService self;

	public OrderService(
			OrderRepository orderRepository,
			OrderItemRepository orderItemRepository,
			MenuItemRepository menuItemRepository,
			RestaurantRepository restaurantRepository,
			UserRepository userRepository,
			PaymentRepository paymentRepository,
			PaymentService paymentService,
			ApplicationEventPublisher eventPublisher) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.menuItemRepository = menuItemRepository;
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
		this.paymentRepository = paymentRepository;
		this.paymentService = paymentService;
		this.eventPublisher = eventPublisher;
	}

	public OrderResponse placeOrder(OrderRequest request, Long customerId) {
		for (int attempt = 1; attempt <= MAX_STOCK_UPDATE_ATTEMPTS; attempt++) {
			try {
				return self.placeOrderInTransaction(request, customerId);
			} catch (OptimisticLockingFailureException ex) {
				if (attempt == MAX_STOCK_UPDATE_ATTEMPTS) {
					throw new InsufficientStockException(
							"Item stock is being updated concurrently; please retry");
				}
			}
		}
		throw new IllegalStateException("Unreachable");
	}

	@Transactional
	public OrderResponse placeOrderInTransaction(OrderRequest request, Long customerId) {
		Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + request.restaurantId()));
		User customer = userRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + customerId));

		BigDecimal totalAmount = BigDecimal.ZERO;
		List<OrderItem> orderItems = new ArrayList<>();

		for (OrderItemRequest itemRequest : request.items()) {
			MenuItem menuItem = menuItemRepository.findById(itemRequest.menuItemId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Menu item not found: " + itemRequest.menuItemId()));
			if (!menuItem.getRestaurant().getId().equals(request.restaurantId())) {
				throw new ResourceNotFoundException(
						"Menu item " + menuItem.getId() + " does not belong to restaurant " + request.restaurantId());
			}
			if (!menuItem.isAvailable()) {
				throw new InsufficientStockException("Menu item " + menuItem.getId() + " is not available");
			}
			if (menuItem.getStockQuantity() < itemRequest.quantity()) {
				throw new InsufficientStockException(
						"Insufficient stock for menu item " + menuItem.getId());
			}

			BigDecimal priceAtOrderTime = menuItem.getPrice();
			menuItem.setStockQuantity(menuItem.getStockQuantity() - itemRequest.quantity());
			// Flush immediately so a concurrent-update conflict on this row surfaces now,
			// inside the retry loop, rather than being deferred to the final commit flush.
			menuItemRepository.saveAndFlush(menuItem);

			OrderItem orderItem = new OrderItem();
			orderItem.setMenuItem(menuItem);
			orderItem.setQuantity(itemRequest.quantity());
			orderItem.setPriceAtOrderTime(priceAtOrderTime);
			orderItems.add(orderItem);

			totalAmount = totalAmount.add(priceAtOrderTime.multiply(BigDecimal.valueOf(itemRequest.quantity())));
		}

		Order order = new Order();
		order.setCustomer(customer);
		order.setRestaurant(restaurant);
		order.setStatus(OrderStatus.PLACED);
		order.setTotalAmount(totalAmount);
		order = orderRepository.save(order);

		for (OrderItem orderItem : orderItems) {
			orderItem.setOrder(order);
		}
		orderItemRepository.saveAll(orderItems);

		boolean paymentSuccess = paymentService.charge(totalAmount);
		if (!paymentSuccess) {
			throw new PaymentFailedException("Payment failed for order total " + totalAmount);
		}

		Payment payment = new Payment();
		payment.setOrder(order);
		payment.setAmount(totalAmount);
		payment.setStatus(PaymentStatus.SUCCESS);
		payment.setMethod(PaymentService.MOCK_METHOD);
		paymentRepository.save(payment);

		eventPublisher.publishEvent(new OrderStatusChangedEvent(order.getId(), null, OrderStatus.PLACED));

		List<OrderItemResponse> itemResponses = orderItems.stream().map(OrderItemResponse::from).toList();
		return OrderResponse.from(order, itemResponses);
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrder(Long orderId, Long customerId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
		if (!order.getCustomer().getId().equals(customerId)) {
			throw new UnauthorizedActionException("You do not own this order");
		}
		List<OrderItemResponse> items = orderItemRepository.findByOrderId(orderId).stream()
				.map(OrderItemResponse::from)
				.toList();
		return OrderResponse.from(order, items);
	}
}
