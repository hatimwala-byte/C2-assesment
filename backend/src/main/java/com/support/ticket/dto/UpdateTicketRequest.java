package com.support.ticket.dto;

import java.util.Optional;

import com.support.ticket.domain.TicketPriority;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
		Optional<@NotBlank @Size(max = 200) String> title,
		Optional<@NotBlank @Size(max = 5000) String> description,
		Optional<TicketPriority> priority,
		@Size(max = 100) String assignee,
		boolean assigneePresent) {

	@AssertTrue(message = "At least one field must be provided")
	public boolean hasAtLeastOneField() {
		return title.isPresent() || description.isPresent() || priority.isPresent() || assigneePresent;
	}

}
