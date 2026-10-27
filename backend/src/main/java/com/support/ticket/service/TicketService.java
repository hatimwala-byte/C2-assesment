package com.support.ticket.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.support.ticket.domain.Ticket;
import com.support.ticket.domain.TicketPriority;
import com.support.ticket.domain.TicketStatus;
import com.support.ticket.domain.exception.InvalidQueryParameterException;
import com.support.ticket.domain.exception.NoFieldsToUpdateException;
import com.support.ticket.domain.exception.ResourceNotFoundException;
import com.support.ticket.domain.policy.TicketStatusTransitionPolicy;
import com.support.ticket.repository.TicketSpecifications;
import com.support.ticket.dto.CreateTicketRequest;
import com.support.ticket.dto.TicketResponse;
import com.support.ticket.dto.UpdateTicketRequest;
import com.support.ticket.dto.UpdateTicketStatusRequest;
import com.support.ticket.repository.TicketRepository;

@Service
public class TicketService {

	private static final String TICKET_RESOURCE = "Ticket";

	private final TicketRepository ticketRepository;

	private final TicketStatusTransitionPolicy transitionPolicy;

	private final TicketMapper ticketMapper;

	public TicketService(
			TicketRepository ticketRepository,
			TicketStatusTransitionPolicy transitionPolicy,
			TicketMapper ticketMapper) {
		this.ticketRepository = ticketRepository;
		this.transitionPolicy = transitionPolicy;
		this.ticketMapper = ticketMapper;
	}

	@Transactional
	public TicketResponse create(CreateTicketRequest request) {
		Ticket ticket = new Ticket(request.title().trim(), request.description().trim());
		ticket.setPriority(request.priority() != null ? request.priority() : TicketPriority.MEDIUM);
		ticket.setAssignee(normalizeAssignee(request.assignee()));
		ticket.setStatus(TicketStatus.OPEN);
		return ticketMapper.toResponse(ticketRepository.save(ticket));
	}

	@Transactional(readOnly = true)
	public TicketResponse getById(Long id) {
		return ticketMapper.toResponse(findTicketOrThrow(id));
	}

	@Transactional(readOnly = true)
	public Page<TicketResponse> listTickets(
			String query,
			List<String> statusTokens,
			List<String> priorityTokens,
			String assignee,
			Pageable pageable) {
		List<TicketStatus> statuses = parseStatusFilter(statusTokens);
		List<TicketPriority> priorities = parsePriorityFilter(priorityTokens);
		String keyword = normalizeKeyword(query);
		String assigneeFilter = normalizeAssigneeFilter(assignee);
		Specification<Ticket> specification = Specification.where(TicketSpecifications.matchesKeyword(keyword))
				.and(TicketSpecifications.hasStatusIn(statuses))
				.and(TicketSpecifications.hasPriorityIn(priorities))
				.and(TicketSpecifications.hasAssignee(assigneeFilter));
		return ticketRepository.findAll(specification, pageable).map(ticketMapper::toResponse);
	}

	@Transactional
	public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
		if (!request.hasAtLeastOneField()) {
			throw new NoFieldsToUpdateException();
		}

		Ticket ticket = findTicketOrThrow(id);

		request.title().ifPresent(ticket::setTitle);
		request.description().ifPresent(ticket::setDescription);
		request.priority().ifPresent(ticket::setPriority);
		if (request.assigneePresent()) {
			ticket.setAssignee(normalizeAssignee(request.assignee()));
		}

		return ticketMapper.toResponse(ticketRepository.save(ticket));
	}

	@Transactional
	public TicketResponse updateStatus(Long id, UpdateTicketStatusRequest request) {
		Ticket ticket = findTicketOrThrow(id);
		TicketStatus targetStatus = request.status();

		if (ticket.getStatus() == targetStatus) {
			return ticketMapper.toResponse(ticket);
		}

		transitionPolicy.validateTransition(ticket.getStatus(), targetStatus);
		ticket.setStatus(targetStatus);
		return ticketMapper.toResponse(ticketRepository.save(ticket));
	}

	private Ticket findTicketOrThrow(Long id) {
		return ticketRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(TICKET_RESOURCE, id));
	}

	private static String normalizeAssignee(String assignee) {
		if (assignee == null) {
			return null;
		}
		String trimmed = assignee.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String normalizeKeyword(String query) {
		if (query == null) {
			return null;
		}
		String trimmed = query.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String normalizeAssigneeFilter(String assignee) {
		if (assignee == null) {
			return null;
		}
		String trimmed = assignee.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static List<TicketPriority> parsePriorityFilter(List<String> priorityTokens) {
		if (priorityTokens == null || priorityTokens.isEmpty()) {
			return null;
		}
		List<TicketPriority> priorities = new ArrayList<>();
		for (String token : priorityTokens) {
			if (token == null || token.isBlank()) {
				continue;
			}
			for (String part : token.split(",")) {
				String trimmed = part.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				try {
					priorities.add(TicketPriority.valueOf(trimmed));
				}
				catch (IllegalArgumentException ex) {
					throw new InvalidQueryParameterException("priority", "Invalid priority value: " + trimmed);
				}
			}
		}
		return priorities.isEmpty() ? null : priorities;
	}

	private static List<TicketStatus> parseStatusFilter(List<String> statusTokens) {
		if (statusTokens == null || statusTokens.isEmpty()) {
			return null;
		}
		List<TicketStatus> statuses = new ArrayList<>();
		for (String token : statusTokens) {
			if (token == null || token.isBlank()) {
				continue;
			}
			for (String part : token.split(",")) {
				String trimmed = part.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				try {
					statuses.add(TicketStatus.valueOf(trimmed));
				}
				catch (IllegalArgumentException ex) {
					throw new InvalidQueryParameterException("status", "Invalid status value: " + trimmed);
				}
			}
		}
		return statuses.isEmpty() ? null : statuses;
	}

}
