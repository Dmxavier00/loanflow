package com.api.loanflow.contrato.aplicacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfContratoServiceTest {

	private static final Charset PDF_CHARSET = Charset.forName("windows-1252");

	@TempDir
	Path tempDir;

	@Test
	void deveGerarPdfMultipaginaComCaracteresLatinos() throws Exception {
		var service = new PdfContratoService(tempDir.toString());
		var conteudo = criarConteudoExtenso();

		var pdfPath = Path.of(service.gerarContratoPdf("LF-TESTE", conteudo)).normalize();
		assertEquals(tempDir.toAbsolutePath().normalize().resolve("LF-TESTE.pdf"), pdfPath);
		assertTrue(Files.exists(pdfPath));

		var pdfBytes = service.lerPdf(pdfPath.toString());
		var pdfText = new String(pdfBytes, PDF_CHARSET);
		assertTrue(pdfText.startsWith("%PDF-1.4"));
		assertTrue(pdfText.contains("MicrocrÃ©dito"));
		assertTrue(Pattern.compile("/Count\\s+[2-9]").matcher(pdfText).find());
	}

	@Test
	void deveLerPdfQuandoReferenciaPersistidaForRelativaAoDiretorioDeContratos() throws Exception {
		var storageDir = tempDir.resolve("contratos");
		var service = new PdfContratoService(storageDir.toString());
		var conteudo = criarConteudoExtenso();

		var pdfPath = Path.of(service.gerarContratoPdf("LF-LEGADO", conteudo)).normalize();
		var legacyRelativePath = Path.of("contratos", pdfPath.getFileName().toString()).toString();

		assertArrayEquals(Files.readAllBytes(pdfPath), service.lerPdf(legacyRelativePath));
	}

	private String criarConteudoExtenso() {
		var conteudo = new StringBuilder()
			.append("Contrato de MicrocrÃ©dito P2P - Instrumento Particular\n\n")
			.append("NÃºmero do contrato: LF-TESTE\n")
			.append("Data de emissÃ£o: 28/04/2026 18:00\n")
			.append("Plataforma emissora: Loanflow\n\n")
			.append("# 1. ClÃ¡usulas simuladas\n");

		for (int i = 1; i <= 90; i++) {
			conteudo
				.append("- ClÃ¡usula ")
				.append(i)
				.append(": esta Ã© uma descriÃ§Ã£o longa do contrato eletrÃ´nico com acentuaÃ§Ã£o vÃ¡lida, revisÃ£o visual e detalhes suficientes para forÃ§ar quebra de pÃ¡gina no PDF.\n");
		}

		return conteudo.toString();
	}
}
