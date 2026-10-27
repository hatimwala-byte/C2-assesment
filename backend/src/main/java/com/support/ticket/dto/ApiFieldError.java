package com.support.ticket.dto;

public record ApiFieldError(String field, Object rejectedValue, String message) {

}
