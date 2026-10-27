package com.support.ticket.domain.exception;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resourceName, long id) {
		super(String.format("%s not found with id: %d", resourceName, id));
	}

}
