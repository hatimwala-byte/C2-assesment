package com.support.ticket.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketPatchUnknownPropertyIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void should_rejectUnknownProperty_when_patchTicket() throws Exception {
		long ticketId = createTicket();

		mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Updated\",\"unexpectedField\":true}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.message").value("Unknown property in request body: unexpectedField"))
				.andExpect(jsonPath("$.errors[0].field").value("unexpectedField"));
	}

	private long createTicket() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/tickets")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Original\",\"description\":\"Desc\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		return body.get("id").asLong();
	}

}
