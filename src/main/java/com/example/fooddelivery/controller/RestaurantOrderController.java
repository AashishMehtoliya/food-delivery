package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.OrderResponse;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.security.AppUserPrincipal;
import com.example.fooddelivery.service.RestaurantOrderService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/restaurants/{restaurantId}/orders")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantOrderController {

	private final RestaurantOrderService restaurantOrderService;

	public RestaurantOrderController(RestaurantOrderService restaurantOrderService) {
		this.restaurantOrderService = restaurantOrderService;
	}

	@GetMapping
	public List<OrderResponse> queue(
			@PathVariable Long restaurantId,
			@RequestParam OrderStatus status,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return restaurantOrderService.listQueue(restaurantId, status, principal.getId());
	}
}
