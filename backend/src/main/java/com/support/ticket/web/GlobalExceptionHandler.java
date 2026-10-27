package com.support.ticket.web;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import com.support.ticket.domain.exception.FieldValidationException;
import com.support.ticket.domain.exception.InvalidQueryParameterException;
import com.support.ticket.domain.exception.InvalidStateTransitionException;
import com.support.ticket.domain.exception.NoFieldsToUpdateException;
import com.support.ticket.domain.exception.ResourceNotFoundException;
import com.support.ticket.domain.exception.StatusUpdateNotAllowedException;
import com.support.ticket.domain.exception.UnknownRequestPropertyException;
import com.support.ticket.dto.ApiErrorResponse;
import com.support.ticket.dto.ApiFieldError;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		List<ApiFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(GlobalExceptionHandler::toApiFieldError)
				.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Validation failed for request payload.", errors));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ticket not found.", List.of()));
	}

	@ExceptionHandler(InvalidStateTransitionException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", ex.getMessage(), List.of()));
	}

	@ExceptionHandler({
			NoFieldsToUpdateException.class,
			FieldValidationException.class,
			UnknownRequestPropertyException.class,
			InvalidQueryParameterException.class,
			StatusUpdateNotAllowedException.class,
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class,
			ConstraintViolationException.class,
			HandlerMethodValidationException.class
	})
	public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex) {
		if (ex instanceof StatusUpdateNotAllowedException statusEx) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(error(
							HttpStatus.BAD_REQUEST,
							"BAD_REQUEST",
							statusEx.getMessage(),
							List.of(new ApiFieldError(
									"status",
									statusEx.getRejectedValue(),
									"Use PATCH /api/v1/tickets/{ticketId}/status to change status."))));
		}
		if (ex instanceof UnknownRequestPropertyException unknownEx) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(error(
							HttpStatus.BAD_REQUEST,
							"BAD_REQUEST",
							unknownEx.getMessage(),
							List.of(new ApiFieldError(unknownEx.getPropertyName(), null, unknownEx.getMessage()))));
		}
		if (ex instanceof FieldValidationException fieldEx) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(error(
							HttpStatus.BAD_REQUEST,
							"BAD_REQUEST",
							"Validation failed for request payload.",
							List.of(new ApiFieldError(fieldEx.getField(), fieldEx.getRejectedValue(), fieldEx.getMessage()))));
		}
		if (ex instanceof InvalidQueryParameterException queryEx) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(error(
							HttpStatus.BAD_REQUEST,
							"BAD_REQUEST",
							queryEx.getMessage(),
							List.of(new ApiFieldError(queryEx.getParameter(), null, queryEx.getMessage()))));
		}
		if (ex instanceof ConstraintViolationException constraintEx) {
			List<ApiFieldError> fieldErrors = constraintEx.getConstraintViolations().stream()
					.map(GlobalExceptionHandler::toApiFieldError)
					.toList();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(error(
							HttpStatus.BAD_REQUEST,
							"BAD_REQUEST",
							"Validation failed for request payload.",
							fieldErrors));
		}
		String message = ex instanceof NoFieldsToUpdateException
				? ex.getMessage()
				: "The request could not be read or contains invalid parameters.";
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, List.of()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(error(
						HttpStatus.INTERNAL_SERVER_ERROR,
						"INTERNAL_SERVER_ERROR",
						"An unexpected error occurred.",
						List.of()));
	}

	private static ApiFieldError toApiFieldError(FieldError fieldError) {
		return new ApiFieldError(fieldError.getField(), fieldError.getRejectedValue(), fieldError.getDefaultMessage());
	}

	private static ApiFieldError toApiFieldError(ConstraintViolation<?> violation) {
		String field = violation.getPropertyPath().toString();
		return new ApiFieldError(field, violation.getInvalidValue(), violation.getMessage());
	}

	private static ApiErrorResponse error(HttpStatus status, String errorCode, String message, List<ApiFieldError> errors) {
		return new ApiErrorResponse(Instant.now(), status.value(), errorCode, message, errors);
	}

}
