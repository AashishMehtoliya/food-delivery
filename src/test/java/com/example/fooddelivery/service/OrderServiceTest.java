package com.example.fooddelivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fooddelivery.dto.OrderItemRequest;
import com.example.fooddelivery.dto.OrderRequest;
import com.example.fooddelivery.dto.OrderResponse;
import com.example.fooddelivery.entity.City;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.OrderItem;
import com.example.fooddelivery.entity.Payment;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.event.OrderStatusChangedEvent;
import com.example.fooddelivery.exception.InsufficientStockException;
import com.example.fooddelivery.exception.PaymentFailedException;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.OrderItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.PaymentRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private RestaurantRepository restaurantRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentService paymentService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private OrderService orderService;

	private Restaurant restaurant;
	private User customer;
	private MenuItem menuItem;

	@BeforeEach
	void setUp() {
		orderService = new OrderService(
				orderRepository,
				orderItemRepository,
				menuItemRepository,
				restaurantRepository,
				userRepository,
				paymentRepository,
				paymentService,
				eventPublisher);

		City city = new City();
		city.setId(1L);
		city.setName("Testville");

		User owner = new User();
		owner.setId(2L);
		owner.setRole(Role.RESTAURANT_OWNER);

		restaurant = new Restaurant();
		restaurant.setId(10L);
		restaurant.setCity(city);
		restaurant.setOwner(owner);

		customer = new User();
		customer.setId(3L);
		customer.setRole(Role.CUSTOMER);

		menuItem = new MenuItem();
		menuItem.setId(20L);
		menuItem.setRestaurant(restaurant);
		menuItem.setName("Burger");
		menuItem.setPrice(new BigDecimal("50.00"));
		menuItem.setStockQuantity(10);
		menuItem.setAvailable(true);
	}

	@Test
	void placeOrder_happyPath_decrementsStockAndCreatesPaymentAndPublishesEvent() {
		when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
		when(userRepository.findById(3L)).thenReturn(Optional.of(customer));
		when(menuItemRepository.findById(20L)).thenReturn(Optional.of(menuItem));
		when(menuItemRepository.saveAndFlush(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
			Order order = inv.getArgument(0);
			order.setId(100L);
			return order;
		});
		when(orderItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
		when(paymentService.charge(any(BigDecimal.class))).thenReturn(true);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

		OrderRequest request = new OrderRequest(10L, List.of(new OrderItemRequest(20L, 2)));
		OrderResponse response = orderService.placeOrderInTransaction(request, 3L);

		assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
		assertThat(response.totalAmount()).isEqualByComparingTo("100.00");
		assertThat(menuItem.getStockQuantity()).isEqualTo(8);

		verify(paymentRepository).save(any(Payment.class));
		verify(eventPublisher).publishEvent(new OrderStatusChangedEvent(100L, null, OrderStatus.PLACED));
	}

	@Test
	void placeOrder_insufficientStock_throwsAndNeverCreatesOrder() {
		menuItem.setStockQuantity(1);
		when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
		when(userRepository.findById(3L)).thenReturn(Optional.of(customer));
		when(menuItemRepository.findById(20L)).thenReturn(Optional.of(menuItem));

		OrderRequest request = new OrderRequest(10L, List.of(new OrderItemRequest(20L, 5)));

		assertThrows(
				InsufficientStockException.class, () -> orderService.placeOrderInTransaction(request, 3L));

		verify(orderRepository, never()).save(any());
		verify(menuItemRepository, never()).saveAndFlush(any());
	}

	@Test
	void placeOrder_paymentFails_throwsAndNeverPersistsPayment() {
		when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
		when(userRepository.findById(3L)).thenReturn(Optional.of(customer));
		when(menuItemRepository.findById(20L)).thenReturn(Optional.of(menuItem));
		when(menuItemRepository.saveAndFlush(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
			Order order = inv.getArgument(0);
			order.setId(101L);
			return order;
		});
		when(orderItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
		when(paymentService.charge(any(BigDecimal.class))).thenReturn(false);

		OrderRequest request = new OrderRequest(10L, List.of(new OrderItemRequest(20L, 1)));

		assertThrows(PaymentFailedException.class, () -> orderService.placeOrderInTransaction(request, 3L));

		verify(paymentRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}
}
