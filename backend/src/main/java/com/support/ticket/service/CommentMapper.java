package com.support.ticket.service;

import org.springframework.stereotype.Component;

import com.support.ticket.domain.Comment;
import com.support.ticket.dto.CommentResponse;

@Component
public class CommentMapper {

	public CommentResponse toResponse(Comment comment) {
		return new CommentResponse(
				comment.getId(),
				comment.getTicketId(),
				comment.getBody(),
				comment.getCreatedAt());
	}

}
