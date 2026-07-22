package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.OrderItemResponse;
import com.example.fooddelivery.dto.OrderResponse;
import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.entity.DeliveryPartner;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.enums.AssignmentStatus;
import com.example.fooddelivery.enums.AvailabilityStatus;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.event.OrderStatusChangedEvent;
import com.example.fooddelivery.exception.InvalidStateTransitionException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.exception.UnauthorizedActionException;
import com.example.fooddelivery.repository.DeliveryAssignmentRepository;
import com.example.fooddelivery.repository.DeliveryPartnerRepository;
import com.example.fooddelivery.repository.OrderItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryOrderService {

	private static final Set<OrderStatus> ALLOWED_TARGETS =
			EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final DeliveryAssignmentRepository deliveryAssignmentRepository;
	private final DeliveryPartnerRepository deliveryPartnerRepository;
	private final ApplicationEventPublisher eventPublisher;

	public DeliveryOrderService(
			OrderRepository orderRepository,
			OrderItemRepository orderItemRepository,
			DeliveryAssignmentRepository deliveryAssignmentRepository,
			DeliveryPartnerRepository deliveryPartnerRepository,
			ApplicationEventPublisher eventPublisher) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.deliveryAssignmentRepository = deliveryAssignmentRepository;
		this.deliveryPartnerRepository = deliveryPartnerRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public OrderResponse updateStatus(Long orderId, OrderStatus targetStatus, Long callerUserId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

		if (!ALLOWED_TARGETS.contains(targetStatus)) {
			throw new InvalidStateTransitionException(order.getStatus(), targetStatus);
		}

		DeliveryPartner partner = deliveryPartnerRepository.findByUserId(callerUserId)
				.orElseThrow(() -> new ResourceNotFoundException("You are not a registered delivery partner"));

		DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("No assignment found for order: " + orderId));
		if (assignment.getStatus() != AssignmentStatus.ACCEPTED
				|| assignment.getPartner() == null
				|| !assignment.getPartner().getId().equals(partner.getId())) {
			throw new UnauthorizedActionException("You are not the accepted partner for this order");
		}

		OrderStatus oldStatus = order.getStatus();
		order.transitionTo(targetStatus);

		if (targetStatus == OrderStatus.DELIVERED) {
			// The partner is free again once delivery completes; nothing else in the spec's
			// endpoint set ever moves a partner back to AVAILABLE after acceptance.
			partner.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
			deliveryPartnerRepository.save(partner);
		}

		eventPublisher.publishEvent(new OrderStatusChangedEvent(orderId, oldStatus, targetStatus));

		List<OrderItemResponse> items = orderItemRepository.findByOrderId(orderId).stream()
				.map(OrderItemResponse::from)
				.toList();
		return OrderResponse.from(order, items);
	}
}
