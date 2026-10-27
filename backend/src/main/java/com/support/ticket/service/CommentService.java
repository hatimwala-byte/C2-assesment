package com.support.ticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.support.ticket.domain.Comment;
import com.support.ticket.domain.Ticket;
import com.support.ticket.domain.exception.ResourceNotFoundException;
import com.support.ticket.dto.CommentResponse;
import com.support.ticket.dto.CreateCommentRequest;
import com.support.ticket.repository.CommentRepository;
import com.support.ticket.repository.TicketRepository;

@Service
public class CommentService {

	private static final String TICKET_RESOURCE = "Ticket";

	private final TicketRepository ticketRepository;

	private final CommentRepository commentRepository;

	private final CommentMapper commentMapper;

	public CommentService(
			TicketRepository ticketRepository,
			CommentRepository commentRepository,
			CommentMapper commentMapper) {
		this.ticketRepository = ticketRepository;
		this.commentRepository = commentRepository;
		this.commentMapper = commentMapper;
	}

	@Transactional
	public CommentResponse addComment(Long ticketId, CreateCommentRequest request) {
		Ticket ticket = findTicketOrThrow(ticketId);
		Comment comment = new Comment(request.body().trim());
		ticket.addComment(comment);
		return commentMapper.toResponse(commentRepository.save(comment));
	}

	@Transactional(readOnly = true)
	public Page<CommentResponse> listComments(Long ticketId, Pageable pageable) {
		findTicketOrThrow(ticketId);
		return commentRepository.findByTicket_IdOrderByCreatedAtAscIdAsc(ticketId, pageable)
				.map(commentMapper::toResponse);
	}

	private Ticket findTicketOrThrow(Long ticketId) {
		return ticketRepository.findById(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException(TICKET_RESOURCE, ticketId));
	}

}
