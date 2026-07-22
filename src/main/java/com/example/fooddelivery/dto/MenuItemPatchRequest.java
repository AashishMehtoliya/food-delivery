package com.example.fooddelivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record MenuItemPatchRequest(
		@DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
		@Min(0) Integer stockQuantity,
		Boolean available) {
}
