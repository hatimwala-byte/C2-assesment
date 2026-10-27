package com.support.ticket.dto;

import com.support.ticket.domain.TicketStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(@NotNull TicketStatus status) {

}
