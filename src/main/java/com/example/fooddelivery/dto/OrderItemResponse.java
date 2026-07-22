package com.example.fooddelivery.dto;

import com.example.fooddelivery.entity.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(Long menuItemId, String menuItemName, int quantity, BigDecimal priceAtOrderTime) {

	public static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(
				item.getMenuItem().getId(),
				item.getMenuItem().getName(),
				item.getQuantity(),
				item.getPriceAtOrderTime());
	}
}
