package com.support.ticket.dto;

import com.support.ticket.domain.TicketPriority;
import com.support.ticket.domain.TicketStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 5000) String description,
		TicketPriority priority,
		@Size(max = 100) String assignee,
		@Null(message = "Use PATCH /api/v1/tickets/{ticketId}/status to change status.")
		TicketStatus status) {

}
