package com.example.fooddelivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderRequest(
		@NotNull Long restaurantId,
		@NotEmpty @Valid List<OrderItemRequest> items) {
}
