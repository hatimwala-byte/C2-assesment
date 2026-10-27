package com.support.ticket.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.ticket.domain.Ticket;
import com.support.ticket.domain.TicketStatus;
import com.support.ticket.repository.TicketRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketStatusTransitionIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TicketRepository ticketRepository;

	@ParameterizedTest(name = "from {0} to {1} -> HTTP {2}, DB {3}")
	@MethodSource("statusTransitionMatrix")
	void should_enforceStateMachine_when_patchStatus(
			TicketStatus from,
			TicketStatus to,
			int expectedHttpStatus,
			TicketStatus expectedDbStatus) throws Exception {
		long ticketId = prepareTicketInState(from);
		TicketStatus statusBeforeRequest = ticketRepository.findById(ticketId).orElseThrow().getStatus();
		assertThat(statusBeforeRequest).isEqualTo(from);

		MvcResult result = mockMvc.perform(patch("/api/v1/tickets/{ticketId}/status", ticketId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"" + to + "\"}"))
				.andReturn();

		assertThat(result.getResponse().getStatus()).isEqualTo(expectedHttpStatus);

		if (expectedHttpStatus == 409) {
			JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(body.get("error").asText()).isEqualTo("INVALID_STATE_TRANSITION");
			assertThat(body.get("message").asText())
					.isEqualTo("Cannot transition ticket from " + from + " to " + to + ".");
		}

		Ticket ticketAfter = ticketRepository.findById(ticketId).orElseThrow();
		assertThat(ticketAfter.getStatus()).isEqualTo(expectedDbStatus);

		if (expectedHttpStatus == 409) {
			assertThat(ticketAfter.getStatus()).isEqualTo(from);
		}
	}

	private static Stream<Arguments> statusTransitionMatrix() {
		return Stream.of(
				Arguments.of(TicketStatus.OPEN, TicketStatus.OPEN, 200, TicketStatus.OPEN),
				Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, 200, TicketStatus.IN_PROGRESS),
				Arguments.of(TicketStatus.OPEN, TicketStatus.RESOLVED, 409, TicketStatus.OPEN),
				Arguments.of(TicketStatus.OPEN, TicketStatus.CLOSED, 409, TicketStatus.OPEN),
				Arguments.of(TicketStatus.OPEN, TicketStatus.CANCELLED, 200, TicketStatus.CANCELLED),
				Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.OPEN, 409, TicketStatus.IN_PROGRESS),
				Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.IN_PROGRESS, 200, TicketStatus.IN_PROGRESS),
				Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, 200, TicketStatus.RESOLVED),
				Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED, 409, TicketStatus.IN_PROGRESS),
				Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED, 200, TicketStatus.CANCELLED),
				Arguments.of(TicketStatus.RESOLVED, TicketStatus.OPEN, 409, TicketStatus.RESOLVED),
				Arguments.of(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS, 409, TicketStatus.RESOLVED),
				Arguments.of(TicketStatus.RESOLVED, TicketStatus.RESOLVED, 200, TicketStatus.RESOLVED),
				Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED, 200, TicketStatus.CLOSED),
				Arguments.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED, 409, TicketStatus.RESOLVED),
				Arguments.of(TicketStatus.CLOSED, TicketStatus.OPEN, 409, TicketStatus.CLOSED),
				Arguments.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS, 409, TicketStatus.CLOSED),
				Arguments.of(TicketStatus.CLOSED, TicketStatus.RESOLVED, 409, TicketStatus.CLOSED),
				Arguments.of(TicketStatus.CLOSED, TicketStatus.CLOSED, 200, TicketStatus.CLOSED),
				Arguments.of(TicketStatus.CLOSED, TicketStatus.CANCELLED, 409, TicketStatus.CLOSED),
				Arguments.of(TicketStatus.CANCELLED, TicketStatus.OPEN, 409, TicketStatus.CANCELLED),
				Arguments.of(TicketStatus.CANCELLED, TicketStatus.IN_PROGRESS, 409, TicketStatus.CANCELLED),
				Arguments.of(TicketStatus.CANCELLED, TicketStatus.RESOLVED, 409, TicketStatus.CANCELLED),
				Arguments.of(TicketStatus.CANCELLED, TicketStatus.CLOSED, 409, TicketStatus.CANCELLED),
				Arguments.of(TicketStatus.CANCELLED, TicketStatus.CANCELLED, 200, TicketStatus.CANCELLED));
	}

	private long prepareTicketInState(TicketStatus targetState) throws Exception {
		long ticketId = createOpenTicket();
		if (targetState == TicketStatus.OPEN) {
			return ticketId;
		}
		if (targetState == TicketStatus.IN_PROGRESS) {
			patchStatusExpectOk(ticketId, TicketStatus.IN_PROGRESS);
			return ticketId;
		}
		if (targetState == TicketStatus.RESOLVED) {
			patchStatusExpectOk(ticketId, TicketStatus.IN_PROGRESS);
			patchStatusExpectOk(ticketId, TicketStatus.RESOLVED);
			return ticketId;
		}
		if (targetState == TicketStatus.CLOSED) {
			patchStatusExpectOk(ticketId, TicketStatus.IN_PROGRESS);
			patchStatusExpectOk(ticketId, TicketStatus.RESOLVED);
			patchStatusExpectOk(ticketId, TicketStatus.CLOSED);
			return ticketId;
		}
		if (targetState == TicketStatus.CANCELLED) {
			patchStatusExpectOk(ticketId, TicketStatus.CANCELLED);
			return ticketId;
		}
		throw new IllegalArgumentException("Unsupported target state: " + targetState);
	}

	private long createOpenTicket() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/tickets")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"title\":\"State machine test\",\"description\":\"Integration transition matrix\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private void patchStatusExpectOk(long ticketId, TicketStatus status) throws Exception {
		mockMvc.perform(patch("/api/v1/tickets/{ticketId}/status", ticketId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"" + status + "\"}"))
				.andExpect(status().isOk());
	}

}
