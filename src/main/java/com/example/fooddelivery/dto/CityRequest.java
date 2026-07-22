package com.example.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;

public record CityRequest(@NotBlank String name) {
}
