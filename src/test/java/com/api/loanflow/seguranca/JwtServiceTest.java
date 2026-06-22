package com.api.loanflow.seguranca;

import com.api.loanflow.usuario.dominio.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
	private static final String VALID_SECRET = "loanflow-test-secret-change-me-loanflow-test-secret-change-me";

	@Test
	void shouldRejectBlankSecret() {
		assertThrows(IllegalStateException.class, () -> new JwtService(" ", 30));
	}

	@Test
	void shouldRejectShortSecret() {
		assertThrows(IllegalStateException.class, () -> new JwtService("short-secret", 30));
	}

	@Test
	void shouldGenerateAndParseToken() {
		JwtService jwtService = new JwtService(VALID_SECRET, 30);

		String token = jwtService.gerarToken("usuario@loanflow.test", Role.SOLICITANTE);

		assertEquals("usuario@loanflow.test", jwtService.extrairEmail(token));
	}
}
