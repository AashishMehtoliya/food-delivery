package com.example.fooddelivery.dto;

import com.example.fooddelivery.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {
}
