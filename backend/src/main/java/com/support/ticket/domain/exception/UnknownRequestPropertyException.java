package com.support.ticket.domain.exception;

public class UnknownRequestPropertyException extends RuntimeException {

	private final String propertyName;

	public UnknownRequestPropertyException(String propertyName) {
		super("Unknown property in request body: " + propertyName);
		this.propertyName = propertyName;
	}

	public String getPropertyName() {
		return propertyName;
	}

}
