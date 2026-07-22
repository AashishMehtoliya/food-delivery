package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.AssignmentResponse;
import com.example.fooddelivery.enums.AssignmentStatus;
import com.example.fooddelivery.security.AppUserPrincipal;
import com.example.fooddelivery.service.DeliveryAssignmentService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('DELIVERY_PARTNER')")
public class DeliveryAssignmentController {

	private final DeliveryAssignmentService deliveryAssignmentService;

	public DeliveryAssignmentController(DeliveryAssignmentService deliveryAssignmentService) {
		this.deliveryAssignmentService = deliveryAssignmentService;
	}

	@GetMapping("/delivery-partners/{partnerId}/assignments")
	public List<AssignmentResponse> assignments(
			@PathVariable Long partnerId,
			@RequestParam AssignmentStatus status,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return deliveryAssignmentService.list(partnerId, status, principal.getId());
	}

	@PostMapping("/orders/{orderId}/assignments/accept")
	public AssignmentResponse accept(
			@PathVariable Long orderId, @AuthenticationPrincipal AppUserPrincipal principal) {
		return deliveryAssignmentService.accept(orderId, principal.getId());
	}
}
