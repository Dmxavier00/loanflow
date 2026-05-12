package com.api.loanflow.shared.frontend;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FrontendProjectPaths {
	private FrontendProjectPaths() {
	}

	public static Path resolveProjectRoot() {
		Path current = Paths.get("").toAbsolutePath().normalize();

		while (current != null) {
			if (isProjectRoot(current)) {
				return current;
			}
			current = current.getParent();
		}

		return Paths.get("").toAbsolutePath().normalize();
	}

	public static Path resolveFrontendDirectory() {
		return resolveProjectRoot().resolve("frontend");
	}

	public static Path resolveDistDirectory() {
		return resolveFrontendDirectory().resolve("dist");
	}

	public static Path resolveDistIndex() {
		return resolveDistDirectory().resolve("index.html");
	}

	private static boolean isProjectRoot(Path path) {
		return Files.exists(path.resolve("pom.xml")) && Files.exists(path.resolve("frontend").resolve("package.json"));
	}
}
