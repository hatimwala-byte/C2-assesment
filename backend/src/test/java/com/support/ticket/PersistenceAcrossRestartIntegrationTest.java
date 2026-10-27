package com.support.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PERSIST-01 / AC-02: file-backed H2 data survives a full JVM context shutdown and restart.
 */
class PersistenceAcrossRestartIntegrationTest {

	@TempDir
	Path databaseDirectory;

	@Test
	void ticketAndComment_surviveApplicationRestart() throws Exception {
		String jdbcUrl = "jdbc:h2:file:" + databaseDirectory.resolve("tickets")
				+ ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";

		long ticketId;
		String title = "Persistence check ticket";
		String description = "Created before simulated restart.";
		String commentBody = "Comment before restart.";

		try (ConfigurableApplicationContext firstContext = startApplication(jdbcUrl)) {
			MockMvc mockMvc = mockMvcFor(firstContext);
			ObjectMapper objectMapper = firstContext.getBean(ObjectMapper.class);

			var createTicket = mockMvc.perform(post("/api/v1/tickets")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "title": "%s",
									  "description": "%s",
									  "priority": "HIGH",
									  "assignee": "team-persistence"
									}
									""".formatted(title, description)))
					.andExpect(status().isCreated())
					.andReturn();

			JsonNode ticketJson = objectMapper.readTree(createTicket.getResponse().getContentAsString());
			ticketId = ticketJson.get("id").asLong();

			mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"body\":\"" + commentBody + "\"}"))
					.andExpect(status().isCreated());
		}

		try (ConfigurableApplicationContext secondContext = startApplication(jdbcUrl)) {
			MockMvc mockMvc = mockMvcFor(secondContext);
			ObjectMapper objectMapper = secondContext.getBean(ObjectMapper.class);

			var getTicket = mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode ticket = objectMapper.readTree(getTicket.getResponse().getContentAsString());
			assertThat(ticket.get("title").asText()).isEqualTo(title);
			assertThat(ticket.get("description").asText()).isEqualTo(description);
			assertThat(ticket.get("status").asText()).isEqualTo("OPEN");
			assertThat(ticket.get("assignee").asText()).isEqualTo("team-persistence");

			var getComments = mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticketId))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode page = objectMapper.readTree(getComments.getResponse().getContentAsString());
			assertThat(page.get("content")).hasSize(1);
			assertThat(page.get("content").get(0).get("body").asText()).isEqualTo(commentBody);
		}
	}

	private static ConfigurableApplicationContext startApplication(String jdbcUrl) {
		// Command-line args override src/test/resources/application.yml (in-memory test DB).
		return new SpringApplicationBuilder(TicketApiApplication.class)
				.run(
						"--spring.profiles.active=h2",
						"--spring.datasource.url=" + jdbcUrl,
						"--spring.datasource.username=sa",
						"--spring.datasource.password=",
						"--spring.datasource.driver-class-name=org.h2.Driver",
						"--spring.flyway.enabled=true",
						"--spring.jpa.hibernate.ddl-auto=validate",
						"--spring.jpa.open-in-view=false");
	}

	private static MockMvc mockMvcFor(ConfigurableApplicationContext context) {
		return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
	}
}
