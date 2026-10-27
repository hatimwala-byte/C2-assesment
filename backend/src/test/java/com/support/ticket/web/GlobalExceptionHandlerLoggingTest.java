package com.support.ticket.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import com.support.ticket.dto.ApiErrorResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class GlobalExceptionHandlerLoggingTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	private ListAppender<ILoggingEvent> logAppender;

	private Logger logger;

	@BeforeEach
	void attachLogAppender() {
		logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
	}

	@AfterEach
	void detachLogAppender() {
		logger.detachAppender(logAppender);
	}

	@Test
	void should_logError_when_handleUnexpected() {
		ResponseEntity<ApiErrorResponse> response =
				handler.handleUnexpected(new IllegalStateException("simulated failure"));

		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().error()).isEqualTo("INTERNAL_SERVER_ERROR");
		assertThat(logAppender.list)
				.anyMatch(event -> event.getLevel() == Level.ERROR && event.getThrowableProxy() != null);
	}

}
