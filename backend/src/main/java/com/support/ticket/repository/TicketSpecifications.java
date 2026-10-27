package com.support.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.support.ticket.domain.Ticket;
import com.support.ticket.domain.TicketPriority;
import com.support.ticket.domain.TicketStatus;

public final class TicketSpecifications {

	private TicketSpecifications() {
	}

	public static Specification<Ticket> matchesKeyword(String keyword) {
		return (root, query, criteriaBuilder) -> {
			if (keyword == null) {
				return criteriaBuilder.conjunction();
			}
			String pattern = "%" + keyword.toLowerCase() + "%";
			return criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern));
		};
	}

	public static Specification<Ticket> hasStatusIn(List<TicketStatus> statuses) {
		return (root, query, criteriaBuilder) -> {
			if (statuses == null || statuses.isEmpty()) {
				return criteriaBuilder.conjunction();
			}
			return root.get("status").in(statuses);
		};
	}

	public static Specification<Ticket> hasPriorityIn(List<TicketPriority> priorities) {
		return (root, query, criteriaBuilder) -> {
			if (priorities == null || priorities.isEmpty()) {
				return criteriaBuilder.conjunction();
			}
			return root.get("priority").in(priorities);
		};
	}

	public static Specification<Ticket> hasAssignee(String assignee) {
		return (root, query, criteriaBuilder) -> {
			if (assignee == null) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.equal(root.get("assignee"), assignee);
		};
	}

}
