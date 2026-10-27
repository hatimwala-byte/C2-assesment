package com.support.ticket.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.support.ticket.domain.exception.InvalidQueryParameterException;

public final class PageableSupport {

	private PageableSupport() {
	}

	public static Pageable forTickets(Pageable pageable) {
		validatePageAndSize(pageable);
		Sort sort = resolveSort(pageable.getSort());
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}

	public static Pageable forComments(Pageable pageable) {
		validatePageAndSize(pageable);
		Sort sort = resolveCommentSort(pageable.getSort());
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}

	private static void validatePageAndSize(Pageable pageable) {
		if (pageable.getPageNumber() < 0) {
			throw new InvalidQueryParameterException("page", "Page index must be greater than or equal to 0.");
		}
		if (pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
			throw new InvalidQueryParameterException("size", "Page size must be between 1 and 100.");
		}
	}

	private static Sort resolveSort(Sort sort) {
		if (sort.isUnsorted()) {
			return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
		}
		for (Sort.Order order : sort) {
			if (!"createdAt".equals(order.getProperty())) {
				throw new InvalidQueryParameterException("sort", "Only sorting by createdAt is supported.");
			}
		}
		return sort.and(Sort.by(Sort.Order.desc("id")));
	}

	private static Sort resolveCommentSort(Sort sort) {
		if (sort.isUnsorted()) {
			return Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
		}
		for (Sort.Order order : sort) {
			if (!"createdAt".equals(order.getProperty())) {
				throw new InvalidQueryParameterException("sort", "Only sorting by createdAt is supported.");
			}
		}
		return sort.and(Sort.by(Sort.Order.asc("id")));
	}

}
