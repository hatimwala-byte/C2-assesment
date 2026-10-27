package com.support.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.support.ticket.domain.Ticket;
import com.support.ticket.domain.TicketPriority;
import com.support.ticket.domain.TicketStatus;
import com.support.ticket.domain.exception.InvalidQueryParameterException;
import com.support.ticket.domain.exception.InvalidStateTransitionException;
import com.support.ticket.domain.exception.NoFieldsToUpdateException;
import com.support.ticket.domain.exception.ResourceNotFoundException;
import com.support.ticket.domain.policy.TicketStatusTransitionPolicy;
import com.support.ticket.dto.CreateTicketRequest;
import com.support.ticket.dto.TicketResponse;
import com.support.ticket.dto.UpdateTicketRequest;
import com.support.ticket.dto.UpdateTicketStatusRequest;
import com.support.ticket.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private TicketStatusTransitionPolicy transitionPolicy;

	@InjectMocks
	private TicketMapper ticketMapper;

	private TicketService ticketService;

	@BeforeEach
	void setUp() {
		ticketService = new TicketService(ticketRepository, transitionPolicy, ticketMapper);
	}

	@Test
	void should_createOpenTicketWithMediumPriority_when_priorityOmitted() {
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> persistWithId(invocation.getArgument(0), 1L));

		TicketResponse response = ticketService.create(
				new CreateTicketRequest("  Title  ", "  Description  ", null, "  Ada  ", null));

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.title()).isEqualTo("Title");
		assertThat(response.description()).isEqualTo("Description");
		assertThat(response.priority()).isEqualTo(TicketPriority.MEDIUM);
		assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
		assertThat(response.assignee()).isEqualTo("Ada");
	}

	@Test
	void should_useProvidedPriority_when_createTicket() {
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> persistWithId(invocation.getArgument(0), 2L));

		TicketResponse response = ticketService.create(
				new CreateTicketRequest("Title", "Description", TicketPriority.HIGH, null, null));

		assertThat(response.priority()).isEqualTo(TicketPriority.HIGH);
		assertThat(response.assignee()).isNull();
	}

	@Test
	void should_returnTicket_when_getByIdExists() {
		Ticket ticket = sampleTicket(10L, TicketStatus.OPEN);
		when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

		TicketResponse response = ticketService.getById(10L);

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.title()).isEqualTo("Sample");
	}

	@Test
	void should_throwNotFound_when_getByIdMissing() {
		when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ticketService.getById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Ticket not found with id: 99");
	}

	@Test
	void should_returnPagedTickets_when_listTickets() {
		Ticket ticket = sampleTicket(3L, TicketStatus.RESOLVED);
		Page<Ticket> page = new PageImpl<>(List.of(ticket), PageRequest.of(0, 20), 1);
		when(ticketRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20)))).thenReturn(page);

		Page<TicketResponse> responses =
				ticketService.listTickets(null, null, null, null, PageRequest.of(0, 20));

		assertThat(responses.getTotalElements()).isEqualTo(1);
		assertThat(responses.getContent().get(0).status()).isEqualTo(TicketStatus.RESOLVED);
	}

	@Test
	void should_updateFieldsWithoutChangingStatus_when_patchTicket() {
		Ticket ticket = sampleTicket(5L, TicketStatus.CLOSED);
		when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		UpdateTicketRequest request = new UpdateTicketRequest(
				Optional.of("New title"),
				Optional.empty(),
				Optional.of(TicketPriority.LOW),
				null,
				false);

		TicketResponse response = ticketService.updateTicket(5L, request);

		assertThat(response.title()).isEqualTo("New title");
		assertThat(response.priority()).isEqualTo(TicketPriority.LOW);
		assertThat(response.status()).isEqualTo(TicketStatus.CLOSED);
		verify(ticketRepository).save(ticket);
	}

	@Test
	void should_clearAssignee_when_assigneePresentWithBlankValue() {
		Ticket ticket = sampleTicket(6L, TicketStatus.OPEN);
		ticket.setAssignee("previous");
		when(ticketRepository.findById(6L)).thenReturn(Optional.of(ticket));
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		TicketResponse response = ticketService.updateTicket(
				6L,
				new UpdateTicketRequest(Optional.empty(), Optional.empty(), Optional.empty(), "   ", true));

		assertThat(response.assignee()).isNull();
	}

	@Test
	void should_throwNoFieldsToUpdate_when_patchHasNoRecognizedFields() {
		UpdateTicketRequest request = new UpdateTicketRequest(
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				null,
				false);

		assertThatThrownBy(() -> ticketService.updateTicket(1L, request))
				.isInstanceOf(NoFieldsToUpdateException.class);

		verifyNoInteractions(ticketRepository);
	}

	@Test
	void should_throwNotFound_when_updateTicketMissing() {
		when(ticketRepository.findById(404L)).thenReturn(Optional.empty());

		UpdateTicketRequest request = new UpdateTicketRequest(
				Optional.of("Title"),
				Optional.empty(),
				Optional.empty(),
				null,
				false);

		assertThatThrownBy(() -> ticketService.updateTicket(404L, request))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void should_changeStatus_when_transitionAllowed() {
		Ticket ticket = sampleTicket(7L, TicketStatus.OPEN);
		when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		TicketResponse response = ticketService.updateStatus(
				7L,
				new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS));

		assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
		verify(transitionPolicy).validateTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
		verify(ticketRepository).save(ticket);
	}

	@Test
	void should_notPersist_when_statusUpdateIsIdempotent() {
		Ticket ticket = sampleTicket(8L, TicketStatus.RESOLVED);
		when(ticketRepository.findById(8L)).thenReturn(Optional.of(ticket));

		TicketResponse response = ticketService.updateStatus(
				8L,
				new UpdateTicketStatusRequest(TicketStatus.RESOLVED));

		assertThat(response.status()).isEqualTo(TicketStatus.RESOLVED);
		verify(transitionPolicy, never()).validateTransition(any(), any());
		verify(ticketRepository, never()).save(any());
	}

	@Test
	void should_throwInvalidTransition_when_statusChangeForbidden() {
		Ticket ticket = sampleTicket(9L, TicketStatus.CLOSED);
		when(ticketRepository.findById(9L)).thenReturn(Optional.of(ticket));
		doThrow(new InvalidStateTransitionException(TicketStatus.CLOSED, TicketStatus.OPEN))
				.when(transitionPolicy)
				.validateTransition(TicketStatus.CLOSED, TicketStatus.OPEN);

		assertThatThrownBy(() -> ticketService.updateStatus(
						9L,
						new UpdateTicketStatusRequest(TicketStatus.OPEN)))
				.isInstanceOf(InvalidStateTransitionException.class);

		verify(ticketRepository, never()).save(any());
	}

	@Test
	void should_throwInvalidQueryParameter_when_statusFilterUnknown() {
		assertThatThrownBy(() -> ticketService.listTickets(
						null, List.of("NOT_A_STATE"), null, null, PageRequest.of(0, 20)))
				.isInstanceOf(InvalidQueryParameterException.class)
				.hasMessage("Invalid status value: NOT_A_STATE")
				.satisfies(ex -> assertThat(((InvalidQueryParameterException) ex).getParameter()).isEqualTo("status"));

		verifyNoInteractions(ticketRepository);
	}

	@Test
	void should_throwNotFound_when_statusUpdateMissingTicket() {
		when(ticketRepository.findById(11L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ticketService.updateStatus(
						11L,
						new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private static Ticket sampleTicket(long id, TicketStatus status) {
		Ticket ticket = new Ticket("Sample", "Details");
		ticket.setStatus(status);
		ReflectionTestUtils.setField(ticket, "id", id);
		Instant now = Instant.parse("2026-10-26T09:00:00Z");
		ReflectionTestUtils.setField(ticket, "createdAt", now);
		ReflectionTestUtils.setField(ticket, "updatedAt", now);
		return ticket;
	}

	private static Ticket persistWithId(Ticket ticket, long id) {
		ReflectionTestUtils.setField(ticket, "id", id);
		Instant now = Instant.parse("2026-10-26T10:00:00Z");
		ReflectionTestUtils.setField(ticket, "createdAt", now);
		ReflectionTestUtils.setField(ticket, "updatedAt", now);
		return ticket;
	}

}
