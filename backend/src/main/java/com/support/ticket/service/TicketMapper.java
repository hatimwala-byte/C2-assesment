package com.support.ticket.service;

import org.springframework.stereotype.Component;

import com.support.ticket.domain.Ticket;
import com.support.ticket.dto.TicketResponse;

@Component
public class TicketMapper {

	public TicketResponse toResponse(Ticket ticket) {
		return new TicketResponse(
				ticket.getId(),
				ticket.getTitle(),
				ticket.getDescription(),
				ticket.getPriority(),
				ticket.getStatus(),
				ticket.getAssignee(),
				ticket.getCreatedAt(),
				ticket.getUpdatedAt());
	}

}
