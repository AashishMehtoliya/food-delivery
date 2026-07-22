package com.example.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RestaurantRequest(
		@NotNull Long cityId,
		@NotBlank String name,
		@NotNull Long ownerId) {
}
