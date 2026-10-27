package com.support.ticket.dto;

import java.time.Instant;

import com.support.ticket.domain.TicketPriority;
import com.support.ticket.domain.TicketStatus;

public record TicketResponse(
		Long id,
		String title,
		String description,
		TicketPriority priority,
		TicketStatus status,
		String assignee,
		Instant createdAt,
		Instant updatedAt) {

}
