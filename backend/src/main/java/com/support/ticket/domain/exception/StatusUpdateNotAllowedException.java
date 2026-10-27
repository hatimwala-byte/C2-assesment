package com.support.ticket.domain.exception;

public class StatusUpdateNotAllowedException extends RuntimeException {

	private final Object rejectedValue;

	public StatusUpdateNotAllowedException(Object rejectedValue) {
		super("Status cannot be updated via this endpoint.");
		this.rejectedValue = rejectedValue;
	}

	public Object getRejectedValue() {
		return rejectedValue;
	}

}
