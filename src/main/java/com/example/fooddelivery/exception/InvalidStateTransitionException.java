package com.example.fooddelivery.exception;

import com.example.fooddelivery.enums.OrderStatus;

public class InvalidStateTransitionException extends RuntimeException {

	public InvalidStateTransitionException(OrderStatus from, OrderStatus to) {
		super("Cannot transition order from " + from + " to " + to);
	}
}
