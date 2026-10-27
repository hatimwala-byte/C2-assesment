package com.support.ticket.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.support.ticket.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findByTicket_IdOrderByCreatedAtAscIdAsc(Long ticketId);

	Page<Comment> findByTicket_IdOrderByCreatedAtAscIdAsc(Long ticketId, Pageable pageable);

}
