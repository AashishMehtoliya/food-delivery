package com.example.fooddelivery.dto;

import com.example.fooddelivery.entity.MenuItem;
import java.math.BigDecimal;

public record MenuItemResponse(
		Long id, Long restaurantId, String name, BigDecimal price, int stockQuantity, boolean available) {

	public static MenuItemResponse from(MenuItem item) {
		return new MenuItemResponse(
				item.getId(),
				item.getRestaurant().getId(),
				item.getName(),
				item.getPrice(),
				item.getStockQuantity(),
				item.isAvailable());
	}
}
