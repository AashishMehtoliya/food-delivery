package com.example.fooddelivery.dto;

import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.enums.AssignmentStatus;
import java.time.LocalDateTime;

public record AssignmentResponse(
		Long orderId,
		Long restaurantId,
		String restaurantName,
		AssignmentStatus status,
		Long partnerId,
		LocalDateTime offeredAt,
		LocalDateTime acceptedAt) {

	public static AssignmentResponse from(DeliveryAssignment assignment) {
		return new AssignmentResponse(
				assignment.getOrder().getId(),
				assignment.getOrder().getRestaurant().getId(),
				assignment.getOrder().getRestaurant().getName(),
				assignment.getStatus(),
				assignment.getPartner() != null ? assignment.getPartner().getId() : null,
				assignment.getOfferedAt(),
				assignment.getAcceptedAt());
	}
}
