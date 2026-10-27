package com.support.ticket.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.support.ticket.domain.TicketStatus;
import com.support.ticket.domain.exception.InvalidStateTransitionException;

class TicketStatusTransitionPolicyTest {

	private final TicketStatusTransitionPolicy policy = new TicketStatusTransitionPolicy();

	@ParameterizedTest(name = "from {0} to {1} -> allowed={2}")
	@MethodSource("allFromToPairs")
	void should_matchExpectedAllowance_when_evaluatingTransitionMatrix(
			TicketStatus from,
			TicketStatus to,
			boolean expectedAllowed) {
		assertThat(policy.isTransitionAllowed(from, to)).isEqualTo(expectedAllowed);

		if (expectedAllowed) {
			assertThatCode(() -> policy.validateTransition(from, to)).doesNotThrowAnyException();
		}
		else {
			assertThatThrownBy(() -> policy.validateTransition(from, to))
					.isInstanceOf(InvalidStateTransitionException.class)
					.hasMessage("Cannot transition ticket from %s to %s.", from, to)
					.satisfies(ex -> {
						InvalidStateTransitionException transition = (InvalidStateTransitionException) ex;
						assertThat(transition.getCurrentStatus()).isEqualTo(from);
						assertThat(transition.getTargetStatus()).isEqualTo(to);
					});
		}
	}

	private static Stream<Arguments> allFromToPairs() {
		return Stream.of(TicketStatus.values())
				.flatMap(from -> Stream.of(TicketStatus.values())
						.map(to -> Arguments.of(from, to, expectedAllowed(from, to))));
	}

	private static boolean expectedAllowed(TicketStatus from, TicketStatus to) {
		if (from == to) {
			return true;
		}
		return switch (from) {
			case OPEN -> to == TicketStatus.IN_PROGRESS || to == TicketStatus.CANCELLED;
			case IN_PROGRESS -> to == TicketStatus.RESOLVED || to == TicketStatus.CANCELLED;
			case RESOLVED -> to == TicketStatus.CLOSED;
			case CLOSED, CANCELLED -> false;
		};
	}

}
