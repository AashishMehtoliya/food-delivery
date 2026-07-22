package com.example.fooddelivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fooddelivery.dto.AssignmentResponse;
import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.entity.DeliveryPartner;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.AssignmentStatus;
import com.example.fooddelivery.enums.AvailabilityStatus;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.exception.AssignmentAlreadyAcceptedException;
import com.example.fooddelivery.repository.DeliveryAssignmentRepository;
import com.example.fooddelivery.repository.DeliveryPartnerRepository;
import com.example.fooddelivery.repository.OrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeliveryAssignmentServiceTest {

	@Mock
	private DeliveryAssignmentRepository deliveryAssignmentRepository;

	@Mock
	private DeliveryPartnerRepository deliveryPartnerRepository;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private DeliveryAssignmentService service;

	private DeliveryPartner partner;
	private Order order;

	@BeforeEach
	void setUp() {
		service = new DeliveryAssignmentService(
				deliveryAssignmentRepository, deliveryPartnerRepository, orderRepository, eventPublisher);

		User partnerUser = new User();
		partnerUser.setId(7L);

		partner = new DeliveryPartner();
		partner.setId(3L);
		partner.setUser(partnerUser);
		partner.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

		Restaurant restaurant = new Restaurant();
		restaurant.setId(1L);
		restaurant.setName("Diner");

		order = new Order();
		order.setId(100L);
		order.setRestaurant(restaurant);
		order.setStatus(OrderStatus.ACCEPTED);
	}

	@Test
	void accept_winnerPath_marksPartnerBusyAndAdvancesOrder() {
		when(deliveryPartnerRepository.findByUserId(7L)).thenReturn(Optional.of(partner));
		when(deliveryAssignmentRepository.acceptIfOffered(
						eq(100L), eq(partner), eq(AssignmentStatus.ACCEPTED), eq(AssignmentStatus.OFFERED), any()))
				.thenReturn(1);
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

		DeliveryAssignment resultingAssignment = new DeliveryAssignment();
		resultingAssignment.setOrder(order);
		resultingAssignment.setStatus(AssignmentStatus.ACCEPTED);
		resultingAssignment.setPartner(partner);
		when(deliveryAssignmentRepository.findByOrderId(100L)).thenReturn(Optional.of(resultingAssignment));

		AssignmentResponse response = service.accept(100L, 7L);

		assertThat(response.status()).isEqualTo(AssignmentStatus.ACCEPTED);
		assertThat(response.partnerId()).isEqualTo(3L);
		assertThat(partner.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.BUSY);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
		verify(deliveryPartnerRepository).save(partner);
	}

	@Test
	void accept_alreadyTakenPath_throwsAndLeavesPartnerAndOrderUntouched() {
		when(deliveryPartnerRepository.findByUserId(7L)).thenReturn(Optional.of(partner));
		when(deliveryAssignmentRepository.acceptIfOffered(
						eq(100L), eq(partner), eq(AssignmentStatus.ACCEPTED), eq(AssignmentStatus.OFFERED), any()))
				.thenReturn(0);

		assertThrows(AssignmentAlreadyAcceptedException.class, () -> service.accept(100L, 7L));

		assertThat(partner.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
		verify(deliveryPartnerRepository, never()).save(any());
		verify(orderRepository, never()).findById(any());
	}
}
