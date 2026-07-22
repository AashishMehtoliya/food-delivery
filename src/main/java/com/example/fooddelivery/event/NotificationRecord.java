package com.example.fooddelivery.event;

import com.example.fooddelivery.enums.OrderStatus;
import java.time.Instant;

public record NotificationRecord(
		Long orderId, OrderStatus oldStatus, OrderStatus newStatus, String threadName, Instant sentAt) {
}
