package com.support.ticket.domain.exception;

import com.support.ticket.domain.TicketStatus;

public class InvalidStateTransitionException extends RuntimeException {

	private final TicketStatus currentStatus;

	private final TicketStatus targetStatus;

	public InvalidStateTransitionException(TicketStatus currentStatus, TicketStatus targetStatus) {
		super(String.format("Cannot transition ticket from %s to %s.", currentStatus, targetStatus));
		this.currentStatus = currentStatus;
		this.targetStatus = targetStatus;
	}

	public TicketStatus getCurrentStatus() {
		return currentStatus;
	}

	public TicketStatus getTargetStatus() {
		return targetStatus;
	}

}
