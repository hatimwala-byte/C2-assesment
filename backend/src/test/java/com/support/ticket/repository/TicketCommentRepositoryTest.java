package com.support.ticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.support.ticket.domain.Comment;
import com.support.ticket.domain.Ticket;
import com.support.ticket.domain.TicketPriority;
import com.support.ticket.domain.TicketStatus;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:backend-repo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class TicketCommentRepositoryTest {

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Test
	void should_persistTicketWithComments_when_savedAndLoadedById() {
		Ticket ticket = new Ticket("Login issue", "User cannot reset password");
		ticket.setPriority(TicketPriority.HIGH);
		ticket.setStatus(TicketStatus.IN_PROGRESS);
		ticket.setAssignee("Ada Lovelace");
		ticket.addComment(new Comment("Investigating logs"));
		ticket.addComment(new Comment("Found root cause"));

		Ticket saved = ticketRepository.saveAndFlush(ticket);

		Ticket loaded = ticketRepository.findById(saved.getId()).orElseThrow();
		assertThat(loaded.getTitle()).isEqualTo("Login issue");
		assertThat(loaded.getDescription()).isEqualTo("User cannot reset password");
		assertThat(loaded.getPriority()).isEqualTo(TicketPriority.HIGH);
		assertThat(loaded.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
		assertThat(loaded.getAssignee()).isEqualTo("Ada Lovelace");
		assertThat(loaded.getCreatedAt()).isNotNull();
		assertThat(loaded.getUpdatedAt()).isNotNull();

		var comments = commentRepository.findByTicket_IdOrderByCreatedAtAscIdAsc(loaded.getId());
		assertThat(comments).hasSize(2);
		assertThat(comments.get(0).getBody()).isEqualTo("Investigating logs");
		assertThat(comments.get(1).getBody()).isEqualTo("Found root cause");
		assertThat(comments.get(0).getCreatedAt()).isNotNull();
		assertThat(comments.get(1).getCreatedAt()).isNotNull();
	}

}
