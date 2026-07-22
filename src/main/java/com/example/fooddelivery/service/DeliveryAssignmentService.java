package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.AssignmentResponse;
import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.entity.DeliveryPartner;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.enums.AssignmentStatus;
import com.example.fooddelivery.enums.AvailabilityStatus;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.event.OrderStatusChangedEvent;
import com.example.fooddelivery.exception.AssignmentAlreadyAcceptedException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.exception.UnauthorizedActionException;
import com.example.fooddelivery.repository.DeliveryAssignmentRepository;
import com.example.fooddelivery.repository.DeliveryPartnerRepository;
import com.example.fooddelivery.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryAssignmentService {

	private final DeliveryAssignmentRepository deliveryAssignmentRepository;
	private final DeliveryPartnerRepository deliveryPartnerRepository;
	private final OrderRepository orderRepository;
	private final ApplicationEventPublisher eventPublisher;

	public DeliveryAssignmentService(
			DeliveryAssignmentRepository deliveryAssignmentRepository,
			DeliveryPartnerRepository deliveryPartnerRepository,
			OrderRepository orderRepository,
			ApplicationEventPublisher eventPublisher) {
		this.deliveryAssignmentRepository = deliveryAssignmentRepository;
		this.deliveryPartnerRepository = deliveryPartnerRepository;
		this.orderRepository = orderRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public DeliveryAssignment offer(Order order) {
		DeliveryAssignment assignment = new DeliveryAssignment();
		assignment.setOrder(order);
		assignment.setStatus(AssignmentStatus.OFFERED);
		assignment.setOfferedAt(LocalDateTime.now());
		return deliveryAssignmentRepository.save(assignment);
	}

	@Transactional(readOnly = true)
	public List<AssignmentResponse> list(Long partnerId, AssignmentStatus status, Long callerUserId) {
		assertIsPartner(partnerId, callerUserId);
		return deliveryAssignmentRepository.findByStatus(status).stream()
				.map(AssignmentResponse::from)
				.toList();
	}

	@Transactional
	public AssignmentResponse accept(Long orderId, Long callerUserId) {
		DeliveryPartner partner = deliveryPartnerRepository.findByUserId(callerUserId)
				.orElseThrow(() -> new ResourceNotFoundException("You are not a registered delivery partner"));

		// Single atomic conditional UPDATE (Section 10) resolves the race: whichever
		// concurrent request's UPDATE matches status='OFFERED' first wins the row.
		int updated = deliveryAssignmentRepository.acceptIfOffered(
				orderId, partner, AssignmentStatus.ACCEPTED, AssignmentStatus.OFFERED, LocalDateTime.now());
		if (updated == 0) {
			throw new AssignmentAlreadyAcceptedException(
					"Assignment for order " + orderId + " is no longer available");
		}

		partner.setAvailabilityStatus(AvailabilityStatus.BUSY);
		deliveryPartnerRepository.save(partner);

		// The restaurant already accepted the order (creating this OFFERED assignment);
		// once a partner locks it in, kitchen prep begins. No API endpoint elsewhere moves
		// ACCEPTED -> PREPARING, so this acceptance is treated as that trigger.
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
		OrderStatus oldStatus = order.getStatus();
		order.transitionTo(OrderStatus.PREPARING);
		eventPublisher.publishEvent(new OrderStatusChangedEvent(orderId, oldStatus, OrderStatus.PREPARING));

		DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignment not found for order: " + orderId));
		return AssignmentResponse.from(assignment);
	}

	private void assertIsPartner(Long partnerId, Long callerUserId) {
		DeliveryPartner partner = deliveryPartnerRepository.findById(partnerId)
				.orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found: " + partnerId));
		if (!partner.getUser().getId().equals(callerUserId)) {
			throw new UnauthorizedActionException("You can only view your own assignment pool");
		}
	}
}
