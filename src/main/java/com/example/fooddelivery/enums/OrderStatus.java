package com.example.fooddelivery.enums;

import java.util.Set;

public enum OrderStatus {
	PLACED(Set.of("ACCEPTED", "REJECTED", "CANCELLED")),
	ACCEPTED(Set.of("PREPARING")),
	PREPARING(Set.of("OUT_FOR_DELIVERY")),
	OUT_FOR_DELIVERY(Set.of("DELIVERED")),
	DELIVERED(Set.of()),
	REJECTED(Set.of()),
	CANCELLED(Set.of());

	private final Set<String> allowedNextStatuses;

	OrderStatus(Set<String> allowedNextStatuses) {
		this.allowedNextStatuses = allowedNextStatuses;
	}

	public boolean canTransitionTo(OrderStatus next) {
		return allowedNextStatuses.contains(next.name());
	}
}
