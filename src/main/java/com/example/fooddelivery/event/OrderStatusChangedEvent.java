package com.example.fooddelivery.event;

import com.example.fooddelivery.enums.OrderStatus;

public record OrderStatusChangedEvent(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
}
