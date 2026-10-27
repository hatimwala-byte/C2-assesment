package com.support.ticket.domain.exception;

public class InvalidQueryParameterException extends RuntimeException {

	private final String parameter;

	public InvalidQueryParameterException(String parameter, String message) {
		super(message);
		this.parameter = parameter;
	}

	public String getParameter() {
		return parameter;
	}

}
