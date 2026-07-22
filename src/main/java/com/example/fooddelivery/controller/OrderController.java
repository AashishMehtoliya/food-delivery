package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.OrderRequest;
import com.example.fooddelivery.dto.OrderResponse;
import com.example.fooddelivery.dto.OrderStatusUpdateRequest;
import com.example.fooddelivery.security.AppUserPrincipal;
import com.example.fooddelivery.service.DeliveryOrderService;
import com.example.fooddelivery.service.OrderService;
import com.example.fooddelivery.service.RestaurantOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

	private final OrderService orderService;
	private final RestaurantOrderService restaurantOrderService;
	private final DeliveryOrderService deliveryOrderService;

	public OrderController(
			OrderService orderService,
			RestaurantOrderService restaurantOrderService,
			DeliveryOrderService deliveryOrderService) {
		this.orderService = orderService;
		this.restaurantOrderService = restaurantOrderService;
		this.deliveryOrderService = deliveryOrderService;
	}

	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<OrderResponse> placeOrder(
			@Valid @RequestBody OrderRequest request, @AuthenticationPrincipal AppUserPrincipal principal) {
		return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request, principal.getId()));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public OrderResponse getOrder(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
		return orderService.getOrder(id, principal.getId());
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize("hasRole('CUSTOMER')")
	public OrderResponse cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
		return orderService.cancel(id, principal.getId());
	}

	@PostMapping("/{id}/accept")
	@PreAuthorize("hasRole('RESTAURANT_OWNER')")
	public OrderResponse accept(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
		return restaurantOrderService.accept(id, principal.getId());
	}

	@PostMapping("/{id}/reject")
	@PreAuthorize("hasRole('RESTAURANT_OWNER')")
	public OrderResponse reject(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
		return restaurantOrderService.reject(id, principal.getId());
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('DELIVERY_PARTNER')")
	public OrderResponse updateDeliveryStatus(
			@PathVariable Long id,
			@Valid @RequestBody OrderStatusUpdateRequest request,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return deliveryOrderService.updateStatus(id, request.status(), principal.getId());
	}
}
