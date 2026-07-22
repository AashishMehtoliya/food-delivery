package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.OrderItemResponse;
import com.example.fooddelivery.dto.OrderResponse;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.event.OrderStatusChangedEvent;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.exception.UnauthorizedActionException;
import com.example.fooddelivery.repository.OrderItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantOrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final RestaurantRepository restaurantRepository;
	private final DeliveryAssignmentService deliveryAssignmentService;
	private final PaymentService paymentService;
	private final ApplicationEventPublisher eventPublisher;

	public RestaurantOrderService(
			OrderRepository orderRepository,
			OrderItemRepository orderItemRepository,
			RestaurantRepository restaurantRepository,
			DeliveryAssignmentService deliveryAssignmentService,
			PaymentService paymentService,
			ApplicationEventPublisher eventPublisher) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.restaurantRepository = restaurantRepository;
		this.deliveryAssignmentService = deliveryAssignmentService;
		this.paymentService = paymentService;
		this.eventPublisher = eventPublisher;
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> listQueue(Long restaurantId, OrderStatus status, Long ownerId) {
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));
		if (!restaurant.getOwner().getId().equals(ownerId)) {
			throw new UnauthorizedActionException("You do not own this restaurant");
		}
		return orderRepository.findByRestaurantIdAndStatus(restaurantId, status).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public OrderResponse accept(Long orderId, Long ownerId) {
		Order order = findOwnedOrder(orderId, ownerId);
		OrderStatus oldStatus = order.getStatus();
		order.transitionTo(OrderStatus.ACCEPTED);
		deliveryAssignmentService.offer(order);
		eventPublisher.publishEvent(new OrderStatusChangedEvent(order.getId(), oldStatus, OrderStatus.ACCEPTED));
		return toResponse(order);
	}

	@Transactional
	public OrderResponse reject(Long orderId, Long ownerId) {
		Order order = findOwnedOrder(orderId, ownerId);
		OrderStatus oldStatus = order.getStatus();
		order.transitionTo(OrderStatus.REJECTED);
		paymentService.refund(order.getTotalAmount());
		eventPublisher.publishEvent(new OrderStatusChangedEvent(order.getId(), oldStatus, OrderStatus.REJECTED));
		return toResponse(order);
	}

	private Order findOwnedOrder(Long orderId, Long ownerId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
		assertOwns(order, ownerId);
		return order;
	}

	private void assertOwns(Order order, Long ownerId) {
		if (!order.getRestaurant().getOwner().getId().equals(ownerId)) {
			throw new UnauthorizedActionException("You do not own this restaurant");
		}
	}

	private OrderResponse toResponse(Order order) {
		List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
				.map(OrderItemResponse::from)
				.toList();
		return OrderResponse.from(order, items);
	}
}
