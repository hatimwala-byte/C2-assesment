package com.support.ticket.web;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.support.ticket.domain.TicketPriority;
import com.support.ticket.domain.exception.FieldValidationException;
import com.support.ticket.domain.exception.UnknownRequestPropertyException;
import com.support.ticket.domain.exception.NoFieldsToUpdateException;
import com.support.ticket.domain.exception.StatusUpdateNotAllowedException;
import com.support.ticket.dto.UpdateTicketRequest;

@Component
public class UpdateTicketRequestFactory {

	private static final int TITLE_MAX = 200;

	private static final int DESCRIPTION_MAX = 5000;

	private static final int ASSIGNEE_MAX = 100;

	private static final Set<String> ALLOWED_PROPERTIES = Set.of("title", "description", "priority", "assignee");

	public UpdateTicketRequest fromJson(JsonNode body) {
		if (body == null || body.isNull() || !body.isObject() || body.isEmpty()) {
			throw new NoFieldsToUpdateException();
		}
		rejectUnknownProperties(body);
		if (body.has("status")) {
			JsonNode statusNode = body.get("status");
			Object rejected = statusNode.isNull() ? null : statusNode.asText();
			throw new StatusUpdateNotAllowedException(rejected);
		}

		Optional<String> title = optionalTextField(body, "title", TITLE_MAX);
		Optional<String> description = optionalTextField(body, "description", DESCRIPTION_MAX);
		Optional<TicketPriority> priority = optionalPriority(body, "priority");
		boolean assigneePresent = body.has("assignee");
		String assignee = null;
		if (assigneePresent && !body.get("assignee").isNull()) {
			assignee = body.get("assignee").asText();
		}

		if (assigneePresent && assignee != null && assignee.length() > ASSIGNEE_MAX) {
			throw new FieldValidationException("assignee", assignee, "size must be between 0 and " + ASSIGNEE_MAX);
		}

		UpdateTicketRequest request = new UpdateTicketRequest(title, description, priority, assignee, assigneePresent);
		if (!request.hasAtLeastOneField()) {
			throw new NoFieldsToUpdateException();
		}
		return request;
	}

	private static void rejectUnknownProperties(JsonNode body) {
		Iterator<String> fieldNames = body.fieldNames();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			if (!ALLOWED_PROPERTIES.contains(fieldName)) {
				throw new UnknownRequestPropertyException(fieldName);
			}
		}
	}

	private static Optional<String> optionalTextField(JsonNode body, String fieldName, int maxLength) {
		if (!body.has(fieldName) || body.get(fieldName).isNull()) {
			return Optional.empty();
		}
		String raw = body.get(fieldName).asText();
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			throw new FieldValidationException(fieldName, raw, "must not be blank");
		}
		if (trimmed.length() > maxLength) {
			throw new FieldValidationException(
					fieldName, trimmed, "size must be between 1 and " + maxLength);
		}
		return Optional.of(trimmed);
	}

	private static Optional<TicketPriority> optionalPriority(JsonNode body, String fieldName) {
		if (!body.has(fieldName) || body.get(fieldName).isNull()) {
			return Optional.empty();
		}
		try {
			return Optional.of(TicketPriority.valueOf(body.get(fieldName).asText()));
		}
		catch (IllegalArgumentException ex) {
			throw new com.support.ticket.domain.exception.InvalidQueryParameterException(
					fieldName,
					"Invalid priority value: " + body.get(fieldName).asText());
		}
	}

}
