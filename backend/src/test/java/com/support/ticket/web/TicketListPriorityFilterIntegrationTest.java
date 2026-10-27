package com.support.ticket.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketListPriorityFilterIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void should_filterByPriority_when_listTickets() throws Exception {
		mockMvc.perform(post("/api/v1/tickets")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Low ticket\",\"description\":\"d\",\"priority\":\"LOW\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/tickets")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"High ticket\",\"description\":\"d\",\"priority\":\"HIGH\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/tickets").param("priority", "HIGH"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].title").value("High ticket"))
				.andExpect(jsonPath("$.content[0].priority").value("HIGH"));
	}

}
