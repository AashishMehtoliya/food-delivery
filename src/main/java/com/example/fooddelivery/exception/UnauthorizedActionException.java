package com.example.fooddelivery.exception;

public class UnauthorizedActionException extends RuntimeException {

	public UnauthorizedActionException(String message) {
		super(message);
	}
}
