package com.support.ticket.dto;

import java.time.Instant;

public record CommentResponse(Long id, Long ticketId, String body, Instant createdAt) {

}
