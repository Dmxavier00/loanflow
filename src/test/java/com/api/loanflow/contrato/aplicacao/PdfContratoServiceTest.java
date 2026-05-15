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
		assertTrue(pdfText.contains("Microcrédito"));
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
			.append("Contrato de Microcrédito P2P - Instrumento Particular\n\n")
			.append("Número do contrato: LF-TESTE\n")
			.append("Data de emissão: 28/04/2026 18:00\n")
			.append("Plataforma emissora: LoanFlow\n\n")
			.append("# 1. Cláusulas simuladas\n");

		for (int i = 1; i <= 90; i++) {
			conteudo
				.append("- Cláusula ")
				.append(i)
				.append(": esta é uma descrição longa do contrato eletrônico com acentuação válida, revisão visual e detalhes suficientes para forçar quebra de página no PDF.\n");
		}

		return conteudo.toString();
	}
}
