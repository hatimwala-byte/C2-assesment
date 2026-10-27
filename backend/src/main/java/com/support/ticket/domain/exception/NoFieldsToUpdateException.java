package com.support.ticket.domain.exception;

public class NoFieldsToUpdateException extends RuntimeException {

	public NoFieldsToUpdateException() {
		super("At least one field must be provided");
	}

}
