package com.example.fooddelivery.exception;

public class PaymentFailedException extends RuntimeException {

	public PaymentFailedException(String message) {
		super(message);
	}
}
