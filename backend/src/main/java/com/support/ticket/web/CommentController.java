package com.support.ticket.web;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.support.ticket.dto.CommentResponse;
import com.support.ticket.dto.CreateCommentRequest;
import com.support.ticket.dto.PagedResponse;
import com.support.ticket.service.CommentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
@Validated
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@PostMapping
	public ResponseEntity<CommentResponse> addComment(
			@PathVariable @Min(1) Long ticketId,
			@Valid @RequestBody CreateCommentRequest request) {
		CommentResponse created = commentService.addComment(ticketId, request);
		return ResponseEntity.created(URI.create("/api/v1/tickets/" + ticketId + "/comments/" + created.id()))
				.body(created);
	}

	@GetMapping
	public PagedResponse<CommentResponse> listComments(
			@PathVariable @Min(1) Long ticketId,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
		Page<CommentResponse> page = commentService.listComments(ticketId, PageableSupport.forComments(pageable));
		return PagedResponse.from(page);
	}

}
