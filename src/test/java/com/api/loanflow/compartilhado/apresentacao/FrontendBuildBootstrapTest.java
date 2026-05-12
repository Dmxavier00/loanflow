package com.api.loanflow.compartilhado.apresentacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendBuildBootstrapTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldContinueWhenAutomaticFrontendBuildFails() throws IOException {
		Path frontendDir = tempDir.resolve("apresentacao");
		Files.createDirectories(frontendDir.resolve("src"));
		Files.writeString(frontendDir.resolve("package.json"), "{}");

		AtomicBoolean buildTriggered = new AtomicBoolean();

		assertDoesNotThrow(() -> FrontendBuildBootstrap.prepare(
			frontendDir,
			frontendDir.resolve("dist").resolve("index.html"),
			path -> {
				buildTriggered.set(true);
				throw new IllegalStateException("npm indisponivel");
			}
		));

		assertTrue(buildTriggered.get());
	}

	@Test
	void shouldSkipAutomaticBuildWhenFrontendProjectIsMissing() {
		AtomicBoolean buildTriggered = new AtomicBoolean();

		assertDoesNotThrow(() -> FrontendBuildBootstrap.prepare(
			tempDir.resolve("apresentacao"),
			tempDir.resolve("apresentacao").resolve("dist").resolve("index.html"),
			path -> buildTriggered.set(true)
		));

		assertFalse(buildTriggered.get());
	}
}
