package com.support.ticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.support.ticket.domain.Ticket;
import com.support.ticket.domain.TicketStatus;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:backend-search;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class TicketSearchRepositoryTest {

	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt")
			.and(Sort.by(Sort.Direction.DESC, "id"));

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Ticket alphaLoginOpen;

	private Ticket betaPasswordOpen;

	private Ticket gammaClosed;

	@BeforeEach
	void seedTickets() {
		alphaLoginOpen = persistTicket("Alpha login", "General issue", TicketStatus.OPEN, "2026-10-26T12:00:00Z");
		betaPasswordOpen = persistTicket("Beta", "password reset help", TicketStatus.OPEN, "2026-10-26T11:00:00Z");
		gammaClosed = persistTicket("Gamma login", "Closed ticket", TicketStatus.CLOSED, "2026-10-26T10:00:00Z");
	}

	@Test
	void should_matchTitleCaseInsensitively_when_keywordOnly() {
		Specification<Ticket> specification = Specification.where(TicketSpecifications.matchesKeyword("LoGiN"))
				.and(TicketSpecifications.hasStatusIn(null));

		Page<Ticket> page = ticketRepository.findAll(specification, PageRequest.of(0, 20, NEWEST_FIRST));

		assertThat(page.getContent()).extracting(Ticket::getTitle)
				.containsExactly("Alpha login", "Gamma login");
	}

	@Test
	void should_filterByStatus_when_statusOnly() {
		Specification<Ticket> specification = Specification.where(TicketSpecifications.matchesKeyword(null))
				.and(TicketSpecifications.hasStatusIn(List.of(TicketStatus.OPEN)));

		Page<Ticket> page = ticketRepository.findAll(specification, PageRequest.of(0, 20, NEWEST_FIRST));

		assertThat(page.getContent()).extracting(Ticket::getTitle)
				.containsExactly("Alpha login", "Beta");
	}

	@Test
	void should_applyKeywordAndStatusWithAnd_when_bothProvided() {
		Specification<Ticket> specification = Specification.where(TicketSpecifications.matchesKeyword("password"))
				.and(TicketSpecifications.hasStatusIn(List.of(TicketStatus.OPEN)));

		Page<Ticket> page = ticketRepository.findAll(specification, PageRequest.of(0, 20, NEWEST_FIRST));

		assertThat(page.getContent()).extracting(Ticket::getTitle).containsExactly("Beta");
	}

	@Test
	void should_returnEmptyPage_when_noMatches() {
		Specification<Ticket> specification = Specification.where(TicketSpecifications.matchesKeyword("does-not-exist"))
				.and(TicketSpecifications.hasStatusIn(null));

		Page<Ticket> page = ticketRepository.findAll(specification, PageRequest.of(0, 20, NEWEST_FIRST));

		assertThat(page.getContent()).isEmpty();
	}

	@Test
	void should_sortNewestFirstWithStableIdTiebreaker_when_multipleTicketsMatch() {
		Specification<Ticket> specification = Specification.where(TicketSpecifications.matchesKeyword("login"))
				.and(TicketSpecifications.hasStatusIn(null));

		Page<Ticket> page = ticketRepository.findAll(specification, PageRequest.of(0, 20, NEWEST_FIRST));

		assertThat(page.getContent()).extracting(Ticket::getTitle)
				.containsExactly("Alpha login", "Gamma login");
		assertThat(page.getContent().get(0).getCreatedAt()).isAfter(page.getContent().get(1).getCreatedAt());
	}

	private Ticket persistTicket(String title, String description, TicketStatus status, String createdAt) {
		Ticket ticket = new Ticket(title, description);
		ticket.setStatus(status);
		Ticket saved = ticketRepository.saveAndFlush(ticket);
		Instant instant = Instant.parse(createdAt);
		entityManager.getEntityManager()
				.createNativeQuery("UPDATE ticket SET created_at = ?, updated_at = ? WHERE id = ?")
				.setParameter(1, instant)
				.setParameter(2, instant)
				.setParameter(3, saved.getId())
				.executeUpdate();
		entityManager.clear();
		return ticketRepository.findById(saved.getId()).orElseThrow();
	}

}
