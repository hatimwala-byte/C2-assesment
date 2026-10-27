package com.support.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.support.ticket.domain.Comment;
import com.support.ticket.domain.Ticket;
import com.support.ticket.domain.TicketStatus;
import com.support.ticket.domain.exception.ResourceNotFoundException;
import com.support.ticket.dto.CommentResponse;
import com.support.ticket.dto.CreateCommentRequest;
import com.support.ticket.repository.CommentRepository;
import com.support.ticket.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private CommentRepository commentRepository;

	@InjectMocks
	private CommentMapper commentMapper;

	private CommentService commentService;

	@BeforeEach
	void setUp() {
		commentService = new CommentService(ticketRepository, commentRepository, commentMapper);
	}

	@Test
	void should_persistComment_when_ticketExists() {
		Ticket ticket = sampleTicket(42L, TicketStatus.IN_PROGRESS);
		when(ticketRepository.findById(42L)).thenReturn(Optional.of(ticket));
		when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
			Comment comment = invocation.getArgument(0);
			ReflectionTestUtils.setField(comment, "id", 7L);
			ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-10-26T10:15:00Z"));
			return comment;
		});

		CommentResponse response = commentService.addComment(
				42L,
				new CreateCommentRequest("  Reproduced in staging.  "));

		assertThat(response.id()).isEqualTo(7L);
		assertThat(response.ticketId()).isEqualTo(42L);
		assertThat(response.body()).isEqualTo("Reproduced in staging.");
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
		verify(commentRepository).save(any(Comment.class));
	}

	@Test
	void should_throwNotFound_when_addCommentForMissingTicket() {
		when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.addComment(99L, new CreateCommentRequest("Hello")))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Ticket not found with id: 99");

		verifyNoInteractions(commentRepository);
	}

	@Test
	void should_returnPagedComments_when_ticketExists() {
		Ticket ticket = sampleTicket(5L, TicketStatus.OPEN);
		Comment comment = new Comment("First note");
		ticket.addComment(comment);
		ReflectionTestUtils.setField(comment, "id", 1L);
		ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-10-26T11:00:00Z"));

		when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
		Page<Comment> page = new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1);
		when(commentRepository.findByTicket_IdOrderByCreatedAtAscIdAsc(eq(5L), any(PageRequest.class)))
				.thenReturn(page);

		Page<CommentResponse> responses = commentService.listComments(5L, PageRequest.of(0, 20));

		assertThat(responses.getTotalElements()).isEqualTo(1);
		assertThat(responses.getContent().get(0).body()).isEqualTo("First note");
		assertThat(responses.getContent().get(0).ticketId()).isEqualTo(5L);
	}

	@Test
	void should_throwNotFound_when_listCommentsForMissingTicket() {
		when(ticketRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.listComments(404L, PageRequest.of(0, 20)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Ticket not found with id: 404");

		verifyNoInteractions(commentRepository);
	}

	private static Ticket sampleTicket(long id, TicketStatus status) {
		Ticket ticket = new Ticket("Sample", "Details");
		ticket.setStatus(status);
		ReflectionTestUtils.setField(ticket, "id", id);
		return ticket;
	}

}
