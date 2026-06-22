package com.api.loanflow.compartilhado.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(RecursoNaoEncontradoException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiErrorResponse.of(404, "Not Found", exception.getMessage()));
	}

	@ExceptionHandler(RegraNegocioException.class)
	ResponseEntity<ApiErrorResponse> handleBusiness(RegraNegocioException exception) {
		return ResponseEntity.badRequest()
			.body(ApiErrorResponse.of(400, "Bad Request", exception.getMessage()));
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(ApiErrorResponse.of(401, "Unauthorized", "Credenciais inválidas."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		var fields = new LinkedHashMap<String, String>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			fields.put(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return ResponseEntity.badRequest()
			.body(ApiErrorResponse.of(400, "Bad Request", "Dados de entrada inválidos.", fields));
	}
}
