package com.support.ticket.web;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.support.ticket.dto.CreateTicketRequest;
import com.support.ticket.dto.PagedResponse;
import com.support.ticket.dto.TicketResponse;
import com.support.ticket.dto.UpdateTicketStatusRequest;
import com.support.ticket.service.TicketService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/tickets")
@Validated
public class TicketController {

	private final TicketService ticketService;

	private final UpdateTicketRequestFactory updateTicketRequestFactory;

	public TicketController(TicketService ticketService, UpdateTicketRequestFactory updateTicketRequestFactory) {
		this.ticketService = ticketService;
		this.updateTicketRequestFactory = updateTicketRequestFactory;
	}

	@PostMapping
	public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
		TicketResponse created = ticketService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/tickets/" + created.id())).body(created);
	}

	@GetMapping
	public PagedResponse<TicketResponse> listTickets(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String priority,
			@RequestParam(required = false) String assignee,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
					Pageable pageable) {
		List<String> statusTokens = status == null || status.isBlank() ? null : List.of(status);
		List<String> priorityTokens = priority == null || priority.isBlank() ? null : List.of(priority);
		Page<TicketResponse> page = ticketService.listTickets(
				query, statusTokens, priorityTokens, assignee, PageableSupport.forTickets(pageable));
		return PagedResponse.from(page);
	}

	@GetMapping("/{ticketId}")
	public TicketResponse getTicket(@PathVariable @Min(1) Long ticketId) {
		return ticketService.getById(ticketId);
	}

	@PatchMapping("/{ticketId}")
	public TicketResponse updateTicket(@PathVariable @Min(1) Long ticketId, @RequestBody JsonNode body) {
		return ticketService.updateTicket(ticketId, updateTicketRequestFactory.fromJson(body));
	}

	@PatchMapping("/{ticketId}/status")
	public TicketResponse updateTicketStatus(
			@PathVariable @Min(1) Long ticketId,
			@Valid @RequestBody UpdateTicketStatusRequest request) {
		return ticketService.updateStatus(ticketId, request);
	}

}
