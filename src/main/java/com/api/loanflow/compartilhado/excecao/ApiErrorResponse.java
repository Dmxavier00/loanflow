package com.api.loanflow.compartilhado.excecao;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
	LocalDateTime timestamp,
	int status,
	String error,
	String message,
	Map<String, String> fields
) {
	public static ApiErrorResponse of(int status, String error, String message) {
		return new ApiErrorResponse(LocalDateTime.now(), status, error, message, Map.of());
	}

	public static ApiErrorResponse of(int status, String error, String message, Map<String, String> fields) {
		return new ApiErrorResponse(LocalDateTime.now(), status, error, message, fields);
	}
}
