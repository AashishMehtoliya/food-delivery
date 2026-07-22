package com.example.fooddelivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fooddelivery.dto.OrderResponse;
import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.exception.InvalidStateTransitionException;
import com.example.fooddelivery.exception.UnauthorizedActionException;
import com.example.fooddelivery.repository.OrderItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
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
class RestaurantOrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private RestaurantRepository restaurantRepository;

	@Mock
	private DeliveryAssignmentService deliveryAssignmentService;

	@Mock
	private PaymentService paymentService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private RestaurantOrderService service;

	private Order order;
	private static final Long OWNER_ID = 2L;

	@BeforeEach
	void setUp() {
		service = new RestaurantOrderService(
				orderRepository,
				orderItemRepository,
				restaurantRepository,
				deliveryAssignmentService,
				paymentService,
				eventPublisher);

		User owner = new User();
		owner.setId(OWNER_ID);

		Restaurant restaurant = new Restaurant();
		restaurant.setId(1L);
		restaurant.setOwner(owner);

		User customer = new User();
		customer.setId(50L);

		order = new Order();
		order.setId(100L);
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.PLACED);
		order.setTotalAmount(new BigDecimal("50.00"));
	}

	@Test
	void accept_ownedPlacedOrder_transitionsAndOffersAssignment() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		when(deliveryAssignmentService.offer(order)).thenReturn(new DeliveryAssignment());
		when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of());

		OrderResponse response = service.accept(100L, OWNER_ID);

		assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
		verify(deliveryAssignmentService).offer(order);
	}

	@Test
	void accept_notOwner_throwsUnauthorized() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

		assertThrows(UnauthorizedActionException.class, () -> service.accept(100L, 999L));
		verify(deliveryAssignmentService, never()).offer(any());
	}

	@Test
	void accept_orderNotPlaced_throwsInvalidTransition() {
		order.setStatus(OrderStatus.ACCEPTED);
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

		assertThrows(InvalidStateTransitionException.class, () -> service.accept(100L, OWNER_ID));
		verify(deliveryAssignmentService, never()).offer(any());
	}

	@Test
	void reject_ownedPlacedOrder_transitionsAndRefunds() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of());

		OrderResponse response = service.reject(100L, OWNER_ID);

		assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
		verify(paymentService).refund(new BigDecimal("50.00"));
	}

	@Test
	void reject_notOwner_throwsUnauthorized() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

		assertThrows(UnauthorizedActionException.class, () -> service.reject(100L, 999L));
		verify(paymentService, never()).refund(any());
	}
}
