package com.api.loanflow.compartilhado.apresentacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class FrontendBuildBootstrap {
	private static final Logger log = LoggerFactory.getLogger(FrontendBuildBootstrap.class);
	private static final String DISABLE_PROPERTY = "loanflow.frontend.auto-build";
	private static final String DISABLE_ENV = "LOANFLOW_FRONTEND_AUTO_BUILD";

	private FrontendBuildBootstrap() {
	}

	public static void prepare() {
		Path frontendDir = FrontendProjectPaths.resolveFrontendDirectory();
		Path distIndex = FrontendProjectPaths.resolveDistIndex();
		prepare(frontendDir, distIndex, FrontendBuildBootstrap::runFrontendBuild);
	}

	static void prepare(Path frontendDir, Path distIndex, Consumer<Path> buildRunner) {
		if (!isAutoBuildEnabled()) {
			log.info("Build automático do front-end desativado por propriedade/variável de ambiente.");
			return;
		}

		try {
			if (!Files.exists(frontendDir.resolve("package.json"))) {
				log.info("Front-end React não encontrado em {}. Seguiremos apenas com a API.", frontendDir);
				return;
			}

			if (!isBuildRequired(frontendDir, distIndex)) {
				log.info("Front-end já está pronto em {}.", distIndex);
				return;
			}

			buildRunner.accept(frontendDir);
		} catch (RuntimeException exception) {
			log.warn(
				"Não foi possível gerar o build do front-end automaticamente. A API continuará disponível sem exigir a SPA embutida. Motivo: {}",
				exception.getMessage(),
				exception
			);
		}
	}

	private static boolean isAutoBuildEnabled() {
		String propertyValue = System.getProperty(DISABLE_PROPERTY);
		if (propertyValue != null) {
			return Boolean.parseBoolean(propertyValue);
		}

		String envValue = System.getenv(DISABLE_ENV);
		return envValue == null || Boolean.parseBoolean(envValue);
	}

	private static boolean isBuildRequired(Path frontendDir, Path distIndex) {
		if (!Files.exists(distIndex)) {
			return true;
		}

		Instant distModifiedAt = lastModified(distIndex);

		for (Path watchedPath : watchedPaths(frontendDir)) {
			if (!Files.exists(watchedPath)) {
				continue;
			}

			if (lastModifiedRecursive(watchedPath).isAfter(distModifiedAt)) {
				return true;
			}
		}

		return false;
	}

	private static List<Path> watchedPaths(Path frontendDir) {
		return List.of(
			frontendDir.resolve("src"),
			frontendDir.resolve("public"),
			frontendDir.resolve("index.html"),
			frontendDir.resolve("package.json"),
			frontendDir.resolve("package-lock.json"),
			frontendDir.resolve("vite.config.js")
		);
	}

	private static Instant lastModifiedRecursive(Path path) {
		if (Files.isRegularFile(path)) {
			return lastModified(path);
		}

		try (Stream<Path> stream = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
			return stream
				.filter(Files::isRegularFile)
				.map(FrontendBuildBootstrap::lastModified)
				.max(Comparator.naturalOrder())
				.orElse(Instant.EPOCH);
		} catch (IOException exception) {
			throw new IllegalStateException("Não foi possível analisar alterações do front-end em " + path, exception);
		}
	}

	private static Instant lastModified(Path path) {
		try {
			return Files.getLastModifiedTime(path).toInstant();
		} catch (IOException exception) {
			throw new IllegalStateException("Não foi possível ler a data de modificação de " + path, exception);
		}
	}

	private static void runFrontendBuild(Path frontendDir) {
		Path npmCommand = resolveNpmCommand(frontendDir);

		if (!Files.exists(npmCommand) && npmCommand.getParent() != null) {
			throw new IllegalStateException("Não encontrei o executável do npm em " + npmCommand);
		}

		ProcessBuilder processBuilder = new ProcessBuilder(npmCommand.toString(), "run", "build");
		processBuilder.directory(frontendDir.toFile());
		processBuilder.inheritIO();

		Map<String, String> environment = processBuilder.environment();
		prependNodePath(environment, frontendDir);

		log.info("Gerando build do front-end React antes de subir a API...");

		try {
			Process process = processBuilder.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new IllegalStateException("O build do front-end falhou com código " + exitCode + '.');
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Não foi possível iniciar o build do front-end.", exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("O build do front-end foi interrompido.", exception);
		}
	}

	private static Path resolveNpmCommand(Path frontendDir) {
		Path projectRoot = frontendDir.getParent();
		String npmExecutable = isWindows() ? "npm.cmd" : "npm";
		Path localNodeCommand = isWindows()
			? projectRoot.resolve(".tools").resolve("node").resolve("node-v20.20.2-win-x64").resolve(npmExecutable)
			: projectRoot.resolve(".tools").resolve("node").resolve("bin").resolve(npmExecutable);

		if (Files.exists(localNodeCommand)) {
			return localNodeCommand;
		}

		return Path.of(npmExecutable);
	}

	private static void prependNodePath(Map<String, String> environment, Path frontendDir) {
		Path projectRoot = frontendDir.getParent();
		Path localNodeDir = isWindows()
			? projectRoot.resolve(".tools").resolve("node").resolve("node-v20.20.2-win-x64")
			: projectRoot.resolve(".tools").resolve("node").resolve("bin");

		if (!Files.exists(localNodeDir)) {
			return;
		}

		String pathKey = environment.containsKey("Path") ? "Path" : "PATH";
		String currentPath = environment.getOrDefault(pathKey, "");
		environment.put(pathKey, localNodeDir.toAbsolutePath() + java.io.File.pathSeparator + currentPath);
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}
}
