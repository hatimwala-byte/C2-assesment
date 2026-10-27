package com.support.ticket.domain.policy;

import java.util.Set;

import com.support.ticket.domain.TicketStatus;
import org.springframework.stereotype.Component;

import com.support.ticket.domain.exception.InvalidStateTransitionException;

@Component
public class TicketStatusTransitionPolicy {

	private static final Set<StatusTransition> ALLOWED_DISTINCT_TRANSITIONS = Set.of(
			new StatusTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
			new StatusTransition(TicketStatus.OPEN, TicketStatus.CANCELLED),
			new StatusTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
			new StatusTransition(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
			new StatusTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED));

	public boolean isTransitionAllowed(TicketStatus currentStatus, TicketStatus targetStatus) {
		if (currentStatus == targetStatus) {
			return true;
		}
		return ALLOWED_DISTINCT_TRANSITIONS.contains(new StatusTransition(currentStatus, targetStatus));
	}

	public void validateTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
		if (!isTransitionAllowed(currentStatus, targetStatus)) {
			throw new InvalidStateTransitionException(currentStatus, targetStatus);
		}
	}

	private record StatusTransition(TicketStatus from, TicketStatus to) {
	}

}
