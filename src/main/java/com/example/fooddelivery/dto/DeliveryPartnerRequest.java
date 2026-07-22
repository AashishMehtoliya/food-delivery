package com.example.fooddelivery.dto;

import jakarta.validation.constraints.NotNull;

public record DeliveryPartnerRequest(@NotNull Long userId) {
}
