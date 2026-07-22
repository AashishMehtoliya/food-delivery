package com.example.fooddelivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fooddelivery.dto.OrderResponse;
import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.entity.DeliveryPartner;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.AssignmentStatus;
import com.example.fooddelivery.enums.AvailabilityStatus;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.exception.InvalidStateTransitionException;
import com.example.fooddelivery.exception.UnauthorizedActionException;
import com.example.fooddelivery.repository.DeliveryAssignmentRepository;
import com.example.fooddelivery.repository.DeliveryPartnerRepository;
import com.example.fooddelivery.repository.OrderItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeliveryOrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private DeliveryAssignmentRepository deliveryAssignmentRepository;

	@Mock
	private DeliveryPartnerRepository deliveryPartnerRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private DeliveryOrderService service;

	private static final Long PARTNER_USER_ID = 7L;
	private DeliveryPartner partner;
	private Order order;
	private DeliveryAssignment assignment;

	@BeforeEach
	void setUp() {
		service = new DeliveryOrderService(
				orderRepository, orderItemRepository, deliveryAssignmentRepository, deliveryPartnerRepository,
				eventPublisher);

		User partnerUser = new User();
		partnerUser.setId(PARTNER_USER_ID);

		partner = new DeliveryPartner();
		partner.setId(9L);
		partner.setUser(partnerUser);
		partner.setAvailabilityStatus(AvailabilityStatus.BUSY);

		Restaurant restaurant = new Restaurant();
		restaurant.setId(1L);

		User customer = new User();
		customer.setId(50L);

		order = new Order();
		order.setId(100L);
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.PREPARING);

		assignment = new DeliveryAssignment();
		assignment.setOrder(order);
		assignment.setStatus(AssignmentStatus.ACCEPTED);
		assignment.setPartner(partner);
	}

	@Test
	void updateStatus_toOutForDelivery_succeeds() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		when(deliveryPartnerRepository.findByUserId(PARTNER_USER_ID)).thenReturn(Optional.of(partner));
		when(deliveryAssignmentRepository.findByOrderId(100L)).thenReturn(Optional.of(assignment));
		when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of());

		OrderResponse response = service.updateStatus(100L, OrderStatus.OUT_FOR_DELIVERY, PARTNER_USER_ID);

		assertThat(response.status()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
		verify(deliveryPartnerRepository, never()).save(partner);
	}

	@Test
	void updateStatus_toDelivered_freesThePartner() {
		order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		when(deliveryPartnerRepository.findByUserId(PARTNER_USER_ID)).thenReturn(Optional.of(partner));
		when(deliveryAssignmentRepository.findByOrderId(100L)).thenReturn(Optional.of(assignment));
		when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of());

		OrderResponse response = service.updateStatus(100L, OrderStatus.DELIVERED, PARTNER_USER_ID);

		assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
		assertThat(partner.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
		verify(deliveryPartnerRepository).save(partner);
	}

	@Test
	void updateStatus_byNonAcceptedPartner_throwsUnauthorized() {
		User otherUser = new User();
		otherUser.setId(999L);
		DeliveryPartner otherPartner = new DeliveryPartner();
		otherPartner.setId(50L);
		otherPartner.setUser(otherUser);

		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		when(deliveryPartnerRepository.findByUserId(999L)).thenReturn(Optional.of(otherPartner));
		when(deliveryAssignmentRepository.findByOrderId(100L)).thenReturn(Optional.of(assignment));

		assertThrows(
				UnauthorizedActionException.class,
				() -> service.updateStatus(100L, OrderStatus.OUT_FOR_DELIVERY, 999L));
	}

	@Test
	void updateStatus_skippingOutForDelivery_throwsInvalidTransition() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		when(deliveryPartnerRepository.findByUserId(PARTNER_USER_ID)).thenReturn(Optional.of(partner));
		when(deliveryAssignmentRepository.findByOrderId(100L)).thenReturn(Optional.of(assignment));

		assertThrows(
				InvalidStateTransitionException.class,
				() -> service.updateStatus(100L, OrderStatus.DELIVERED, PARTNER_USER_ID));
	}

	@Test
	void updateStatus_disallowedTargetStatus_throwsInvalidTransition() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

		assertThrows(
				InvalidStateTransitionException.class,
				() -> service.updateStatus(100L, OrderStatus.CANCELLED, PARTNER_USER_ID));
	}
}
