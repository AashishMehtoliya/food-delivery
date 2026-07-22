package com.example.fooddelivery.dto;

import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
		Long id,
		Long restaurantId,
		Long customerId,
		OrderStatus status,
		BigDecimal totalAmount,
		List<OrderItemResponse> items,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static OrderResponse from(Order order, List<OrderItemResponse> items) {
		return new OrderResponse(
				order.getId(),
				order.getRestaurant().getId(),
				order.getCustomer().getId(),
				order.getStatus(),
				order.getTotalAmount(),
				items,
				order.getCreatedAt(),
				order.getUpdatedAt());
	}
}
