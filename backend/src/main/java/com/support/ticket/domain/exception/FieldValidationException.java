package com.support.ticket.domain.exception;

public class FieldValidationException extends RuntimeException {

	private final String field;

	private final Object rejectedValue;

	public FieldValidationException(String field, Object rejectedValue, String message) {
		super(message);
		this.field = field;
		this.rejectedValue = rejectedValue;
	}

	public String getField() {
		return field;
	}

	public Object getRejectedValue() {
		return rejectedValue;
	}

}
