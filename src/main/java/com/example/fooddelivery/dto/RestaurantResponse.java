package com.example.fooddelivery.dto;

import com.example.fooddelivery.entity.Restaurant;

public record RestaurantResponse(Long id, String name, Long cityId, String cityName, Long ownerId, String ownerName) {

	public static RestaurantResponse from(Restaurant restaurant) {
		return new RestaurantResponse(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getCity().getId(),
				restaurant.getCity().getName(),
				restaurant.getOwner().getId(),
				restaurant.getOwner().getName());
	}
}
