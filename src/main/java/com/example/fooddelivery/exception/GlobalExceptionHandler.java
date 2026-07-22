package com.example.fooddelivery.exception;

import com.example.fooddelivery.dto.ErrorResponse;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
	}

	@ExceptionHandler(ResourceConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(ResourceConflictException ex) {
		return build(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", ex.getMessage());
	}

	@ExceptionHandler(InvalidRoleAssignmentException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRole(InvalidRoleAssignmentException ex) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_ROLE_ASSIGNMENT", ex.getMessage());
	}

	@ExceptionHandler(InvalidStateTransitionException.class)
	public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex) {
		return build(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", ex.getMessage());
	}

	@ExceptionHandler(InsufficientStockException.class)
	public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
		return build(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", ex.getMessage());
	}

	@ExceptionHandler(PaymentFailedException.class)
	public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex) {
		return build(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", ex.getMessage());
	}

	@ExceptionHandler(AssignmentAlreadyAcceptedException.class)
	public ResponseEntity<ErrorResponse> handleAssignmentAlreadyAccepted(AssignmentAlreadyAcceptedException ex) {
		return build(HttpStatus.CONFLICT, "ASSIGNMENT_ALREADY_TAKEN", ex.getMessage());
	}

	@ExceptionHandler(UnauthorizedActionException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedActionException ex) {
		return build(HttpStatus.FORBIDDEN, "UNAUTHORIZED_ACTION", ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
		return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST_BODY", "Request body is missing or malformed");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return build(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_FAILED",
				"Invalid value for parameter '" + ex.getName() + "': " + ex.getValue());
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), error, message));
	}
}
