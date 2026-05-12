package com.api.loanflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FlywayAutoConfigurationClasspathTest {

	@Test
	void flywayAutoConfigurationDeveEstarDisponivelNoRuntime() {
		assertDoesNotThrow(() -> Class.forName("org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"));
	}
}
