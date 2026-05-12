package com.api.loanflow.contrato.application;

import com.api.loanflow.shared.exception.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PdfContratoService {
	private static final Charset PDF_CHARSET = Charset.forName("windows-1252");
	private static final float PAGE_WIDTH = 595f;
	private static final float PAGE_HEIGHT = 842f;

	private final Path storagePath;

	public PdfContratoService(@Value("${loanflow.contract.storage-path}") String storagePath) {
		this.storagePath = Path.of(storagePath);
	}

	public String gerarContratoPdf(String numeroContrato, String conteudo) {
		try {
			Files.createDirectories(storagePath);
			var fileName = numeroContrato.replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf";
			var destino = storagePath.resolve(fileName);
			Files.write(destino, criarPdfEstruturado(numeroContrato, conteudo));
			return destino.toString();
		} catch (IOException exception) {
			throw new RegraNegocioException("Não foi possível gerar o PDF do contrato.");
		}
	}

	public byte[] lerPdf(String pdfPath) {
		try {
			return Files.readAllBytes(Path.of(pdfPath));
		} catch (IOException exception) {
			throw new RegraNegocioException("Não foi possível ler o PDF do contrato.");
		}
	}

	private byte[] criarPdfEstruturado(String numeroContrato, String conteudo) throws IOException {
		var renderer = new PdfRenderer(numeroContrato);
		var pages = renderer.render(parseBlocks(conteudo));
		return montarPdf(pages);
	}

	private List<Block> parseBlocks(String conteudo) {
		var blocks = new ArrayList<Block>();
		var sanitized = sanitizarConteudo(conteudo).replace("\r\n", "\n").replace('\r', '\n');
		var titleAssigned = false;
		var previousWasSpacer = false;

		for (String rawLine : sanitized.split("\n")) {
			var line = rawLine.strip();
			if (line.isBlank()) {
				if (!previousWasSpacer && !blocks.isEmpty()) {
					blocks.add(new Block(BlockType.SPACER, "", ""));
				}
				previousWasSpacer = true;
				continue;
			}

			previousWasSpacer = false;
			if (!titleAssigned) {
				blocks.add(new Block(BlockType.TITLE, line, ""));
				titleAssigned = true;
				continue;
			}
			if (line.startsWith("# ")) {
				blocks.add(new Block(BlockType.SECTION, line.substring(2).trim(), ""));
				continue;
			}
			if (line.startsWith("## ")) {
				blocks.add(new Block(BlockType.SUBSECTION, line.substring(3).trim(), ""));
				continue;
			}
			if (line.startsWith("- ")) {
				blocks.add(new Block(BlockType.BULLET, line.substring(2).trim(), ""));
				continue;
			}
			if (line.startsWith("> ")) {
				blocks.add(new Block(BlockType.NOTE, line.substring(2).trim(), ""));
				continue;
			}

			var colonIndex = line.indexOf(':');
			if (colonIndex > 0 && colonIndex < 36) {
				blocks.add(new Block(
					BlockType.KEY_VALUE,
					line.substring(0, colonIndex).trim(),
					line.substring(colonIndex + 1).trim()
				));
				continue;
			}

			blocks.add(new Block(BlockType.PARAGRAPH, line, ""));
		}

		if (blocks.isEmpty()) {
			blocks.add(new Block(BlockType.TITLE, "Contrato", ""));
		}

		return blocks;
	}

	private byte[] montarPdf(List<String> pageStreams) throws IOException {
		var objects = new ArrayList<byte[]>();
		objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>\n"));

		var kids = new StringBuilder();
		for (int i = 0; i < pageStreams.size(); i++) {
			if (kids.length() > 0) {
				kids.append(' ');
			}
			kids.append(6 + (i * 2)).append(" 0 R");
		}

		objects.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + pageStreams.size() + " >>\n"));
		objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\n"));
		objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\n"));
		objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique /Encoding /WinAnsiEncoding >>\n"));

		for (String pageStream : pageStreams) {
			var contentBytes = bytes(pageStream);
			objects.add(bytes(
				"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + format(PAGE_WIDTH) + " " + format(PAGE_HEIGHT) + "] " +
					"/Resources << /ProcSet [/PDF /Text] /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> " +
					"/Contents " + (objects.size() + 2) + " 0 R >>\n"
			));
			objects.add(concat(
				bytes("<< /Length " + contentBytes.length + " >>\nstream\n"),
				contentBytes,
				bytes("\nendstream\n")
			));
		}

		var output = new ByteArrayOutputStream();
		output.write(bytes("%PDF-1.4\n%âãÏÓ\n"));
		var offsets = new ArrayList<Integer>();
		for (int i = 0; i < objects.size(); i++) {
			offsets.add(output.size());
			output.write(bytes((i + 1) + " 0 obj\n"));
			output.write(objects.get(i));
			output.write(bytes("endobj\n"));
		}

		var xrefStart = output.size();
		output.write(bytes("xref\n0 " + (objects.size() + 1) + "\n"));
		output.write(bytes("0000000000 65535 f \n"));
		for (Integer offset : offsets) {
			output.write(bytes(String.format(Locale.US, "%010d 00000 n \n", offset)));
		}
		output.write(bytes(
			"trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n" +
				"startxref\n" + xrefStart + "\n%%EOF"
		));
		return output.toByteArray();
	}

	private byte[] concat(byte[]... arrays) {
		var output = new ByteArrayOutputStream();
		for (byte[] array : arrays) {
			try {
				output.write(array);
			} catch (IOException exception) {
				throw new IllegalStateException("Falha inesperada ao montar o PDF.", exception);
			}
		}
		return output.toByteArray();
	}

	private byte[] bytes(String value) {
		return value.getBytes(PDF_CHARSET);
	}

	private String sanitizarConteudo(String texto) {
		var base = texto == null ? "" : texto;
		var normalizado = Normalizer.normalize(base, Normalizer.Form.NFC)
			.replace('\u2013', '-')
			.replace('\u2014', '-')
			.replace('\u2018', '\'')
			.replace('\u2019', '\'')
			.replace('\u201C', '"')
			.replace('\u201D', '"')
			.replace('\u00A0', ' ')
			.replace('\t', ' ');

		var encoder = PDF_CHARSET.newEncoder();
		var sanitized = new StringBuilder();
		for (char caractere : normalizado.toCharArray()) {
			if (caractere == '\n' || caractere == '\r') {
				sanitized.append(caractere);
			} else if (Character.isWhitespace(caractere)) {
				sanitized.append(' ');
			} else if (caractere >= 32 && encoder.canEncode(caractere)) {
				sanitized.append(caractere);
			} else {
				sanitized.append('?');
			}
		}
		return sanitized.toString();
	}

	private String escapePdf(String texto) {
		return sanitizarConteudo(texto)
			.replace("\\", "\\\\")
			.replace("(", "\\(")
			.replace(")", "\\)");
	}

	private String format(float value) {
		return String.format(Locale.US, "%.2f", value);
	}

	private enum BlockType {
		TITLE,
		SECTION,
		SUBSECTION,
		KEY_VALUE,
		BULLET,
		NOTE,
		PARAGRAPH,
		SPACER
	}

	private record Block(BlockType type, String primary, String secondary) {
	}

	private enum PdfFont {
		REGULAR("F1"),
		BOLD("F2"),
		ITALIC("F3");

		private final String resourceName;

		PdfFont(String resourceName) {
			this.resourceName = resourceName;
		}
	}

	private record PdfColor(float red, float green, float blue) {
	}

	private final class PdfRenderer {
		private static final float MARGIN_LEFT = 48f;
		private static final float MARGIN_RIGHT = 48f;
		private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
		private static final float HEADER_HEIGHT = 74f;
		private static final float FOOTER_HEIGHT = 36f;
		private static final float CONTENT_TOP_Y = PAGE_HEIGHT - HEADER_HEIGHT - 32f;
		private static final float CONTENT_BOTTOM_Y = FOOTER_HEIGHT + 20f;
		private static final float KEY_LABEL_WIDTH = 152f;

		private static final PdfColor PRIMARY = new PdfColor(0.11f, 0.18f, 0.33f);
		private static final PdfColor PRIMARY_DARK = new PdfColor(0.08f, 0.12f, 0.23f);
		private static final PdfColor ACCENT = new PdfColor(0.23f, 0.43f, 0.68f);
		private static final PdfColor TEXT = new PdfColor(0.17f, 0.20f, 0.24f);
		private static final PdfColor MUTED = new PdfColor(0.42f, 0.47f, 0.54f);
		private static final PdfColor BORDER = new PdfColor(0.82f, 0.85f, 0.89f);
		private static final PdfColor WHITE = new PdfColor(1f, 1f, 1f);
		private static final PdfColor NOTE_FILL = new PdfColor(0.94f, 0.97f, 0.99f);

		private final String numeroContrato;
		private final List<String> pages = new ArrayList<>();

		private StringBuilder currentPage;
		private float cursorY;
		private int pageNumber;

		private PdfRenderer(String numeroContrato) {
			this.numeroContrato = escapePdf(numeroContrato);
			startPage();
		}

		private List<String> render(List<Block> blocks) {
			for (Block block : blocks) {
				renderBlock(block);
			}
			finishCurrentPage();
			return pages;
		}

		private void renderBlock(Block block) {
			switch (block.type()) {
				case TITLE -> drawTitle(block.primary());
				case SECTION -> drawSection(block.primary());
				case SUBSECTION -> drawSubsection(block.primary());
				case KEY_VALUE -> drawKeyValue(block.primary(), block.secondary());
				case BULLET -> drawBullet(block.primary());
				case NOTE -> drawNote(block.primary());
				case PARAGRAPH -> drawParagraph(block.primary());
				case SPACER -> cursorY -= 8f;
			}
		}

		private void drawTitle(String text) {
			ensureSpace(34f);
			drawText(PdfFont.BOLD, 18f, MARGIN_LEFT, cursorY, text, PRIMARY_DARK);
			cursorY -= 18f;
			drawLine(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY, ACCENT, 1.1f);
			cursorY -= 16f;
		}

		private void drawSection(String text) {
			ensureSpace(28f);
			drawText(PdfFont.BOLD, 12.6f, MARGIN_LEFT, cursorY, text, PRIMARY);
			cursorY -= 8f;
			drawLine(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY, BORDER, 0.9f);
			cursorY -= 12f;
		}

		private void drawSubsection(String text) {
			ensureSpace(22f);
			drawText(PdfFont.BOLD, 11f, MARGIN_LEFT, cursorY, text, PRIMARY_DARK);
			cursorY -= 15f;
		}

		private void drawKeyValue(String label, String value) {
			var wrapped = wrapText(value, CONTENT_WIDTH - KEY_LABEL_WIDTH, 10f, PdfFont.REGULAR);
			for (int i = 0; i < wrapped.size(); i++) {
				ensureSpace(13f);
				if (i == 0) {
					drawText(PdfFont.BOLD, 9.5f, MARGIN_LEFT, cursorY, label + ":", MUTED);
				}
				drawText(PdfFont.REGULAR, 10f, MARGIN_LEFT + KEY_LABEL_WIDTH, cursorY, wrapped.get(i), TEXT);
				cursorY -= 13f;
			}
			cursorY -= 2f;
		}

		private void drawParagraph(String text) {
			drawWrappedLines(wrapText(text, CONTENT_WIDTH, 10.2f, PdfFont.REGULAR), MARGIN_LEFT, 10.2f, 13.8f, PdfFont.REGULAR, TEXT, 5f);
		}

		private void drawBullet(String text) {
			var lines = wrapText(text, CONTENT_WIDTH - 18f, 10.1f, PdfFont.REGULAR);
			for (int i = 0; i < lines.size(); i++) {
				ensureSpace(13.5f);
				if (i == 0) {
					drawText(PdfFont.BOLD, 10.1f, MARGIN_LEFT, cursorY, "•", ACCENT);
				}
				drawText(PdfFont.REGULAR, 10.1f, MARGIN_LEFT + 14f, cursorY, lines.get(i), TEXT);
				cursorY -= 13.5f;
			}
			cursorY -= 3f;
		}

		private void drawNote(String text) {
			var lines = wrapText(text, CONTENT_WIDTH - 20f, 9.6f, PdfFont.ITALIC);
			var boxHeight = (lines.size() * 12.4f) + 16f;
			ensureSpace(boxHeight + 4f);
			var bottom = cursorY - boxHeight + 6f;
			drawFilledAndStrokedRect(MARGIN_LEFT, bottom, CONTENT_WIDTH, boxHeight, NOTE_FILL, BORDER, 0.8f);
			var textY = cursorY - 12f;
			for (String line : lines) {
				drawText(PdfFont.ITALIC, 9.6f, MARGIN_LEFT + 10f, textY, line, PRIMARY_DARK);
				textY -= 12.4f;
			}
			cursorY = bottom - 10f;
		}

		private void drawWrappedLines(
			List<String> lines,
			float x,
			float fontSize,
			float lineHeight,
			PdfFont font,
			PdfColor color,
			float extraAfter
		) {
			for (String line : lines) {
				ensureSpace(lineHeight);
				drawText(font, fontSize, x, cursorY, line, color);
				cursorY -= lineHeight;
			}
			cursorY -= extraAfter;
		}

		private void ensureSpace(float heightNeeded) {
			if (cursorY - heightNeeded < CONTENT_BOTTOM_Y) {
				startPage();
			}
		}

		private void startPage() {
			if (currentPage != null) {
				pages.add(currentPage.toString());
			}
			currentPage = new StringBuilder();
			pageNumber++;
			cursorY = CONTENT_TOP_Y;
			drawPageChrome();
		}

		private void finishCurrentPage() {
			if (currentPage != null) {
				pages.add(currentPage.toString());
				currentPage = null;
			}
		}

		private void drawPageChrome() {
			drawFilledRect(0f, PAGE_HEIGHT - HEADER_HEIGHT, PAGE_WIDTH, HEADER_HEIGHT, PRIMARY);
			drawText(PdfFont.BOLD, 18f, MARGIN_LEFT, PAGE_HEIGHT - 42f, "Loanflow", WHITE);
			drawText(PdfFont.REGULAR, 10.2f, MARGIN_LEFT, PAGE_HEIGHT - 58f, "Contrato eletrônico de microcrédito P2P", WHITE);
			drawFilledRect(PAGE_WIDTH - 188f, PAGE_HEIGHT - 61f, 140f, 24f, ACCENT);
			drawText(PdfFont.BOLD, 9.2f, PAGE_WIDTH - 176f, PAGE_HEIGHT - 46f, numeroContrato, WHITE);
			drawRect(MARGIN_LEFT - 12f, FOOTER_HEIGHT + 16f, CONTENT_WIDTH + 24f, PAGE_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 28f, BORDER, 0.8f);
			drawLine(MARGIN_LEFT, FOOTER_HEIGHT + 12f, PAGE_WIDTH - MARGIN_RIGHT, FOOTER_HEIGHT + 12f, BORDER, 0.8f);
			drawText(PdfFont.REGULAR, 8.5f, MARGIN_LEFT, FOOTER_HEIGHT - 2f, "Documento eletrônico simulado para fins acadêmicos.", MUTED);
			drawText(PdfFont.REGULAR, 8.5f, PAGE_WIDTH - 88f, FOOTER_HEIGHT - 2f, "Página " + pageNumber, MUTED);
		}

		private void drawText(PdfFont font, float size, float x, float y, String text, PdfColor color) {
			if (text == null || text.isBlank()) {
				return;
			}
			currentPage
				.append(fillColor(color))
				.append('\n')
				.append("BT\n")
				.append("/")
				.append(font.resourceName)
				.append(" ")
				.append(format(size))
				.append(" Tf\n")
				.append("1 0 0 1 ")
				.append(format(x))
				.append(" ")
				.append(format(y))
				.append(" Tm\n")
				.append("(")
				.append(escapePdf(text))
				.append(") Tj\nET\n");
		}

		private void drawLine(float x1, float y1, float x2, float y2, PdfColor color, float width) {
			currentPage
				.append(strokeColor(color))
				.append('\n')
				.append(format(width))
				.append(" w\n")
				.append(format(x1))
				.append(" ")
				.append(format(y1))
				.append(" m\n")
				.append(format(x2))
				.append(" ")
				.append(format(y2))
				.append(" l\nS\n");
		}

		private void drawRect(float x, float y, float width, float height, PdfColor color, float strokeWidth) {
			currentPage
				.append(strokeColor(color))
				.append('\n')
				.append(format(strokeWidth))
				.append(" w\n")
				.append(format(x))
				.append(" ")
				.append(format(y))
				.append(" ")
				.append(format(width))
				.append(" ")
				.append(format(height))
				.append(" re S\n");
		}

		private void drawFilledRect(float x, float y, float width, float height, PdfColor fill) {
			currentPage
				.append(fillColor(fill))
				.append('\n')
				.append(format(x))
				.append(" ")
				.append(format(y))
				.append(" ")
				.append(format(width))
				.append(" ")
				.append(format(height))
				.append(" re f\n");
		}

		private void drawFilledAndStrokedRect(
			float x,
			float y,
			float width,
			float height,
			PdfColor fill,
			PdfColor stroke,
			float strokeWidth
		) {
			currentPage
				.append(fillColor(fill))
				.append('\n')
				.append(strokeColor(stroke))
				.append('\n')
				.append(format(strokeWidth))
				.append(" w\n")
				.append(format(x))
				.append(" ")
				.append(format(y))
				.append(" ")
				.append(format(width))
				.append(" ")
				.append(format(height))
				.append(" re B\n");
		}

		private String fillColor(PdfColor color) {
			return format(color.red()) + " " + format(color.green()) + " " + format(color.blue()) + " rg";
		}

		private String strokeColor(PdfColor color) {
			return format(color.red()) + " " + format(color.green()) + " " + format(color.blue()) + " RG";
		}

		private List<String> wrapText(String text, float maxWidth, float fontSize, PdfFont font) {
			var normalized = text == null ? "" : text.trim().replaceAll("\\s+", " ");
			if (normalized.isBlank()) {
				return List.of("");
			}

			var lines = new ArrayList<String>();
			var current = new StringBuilder();
			for (String word : normalized.split(" ")) {
				if (current.length() == 0) {
					appendWord(lines, current, word, maxWidth, fontSize, font);
					continue;
				}

				var candidate = current + " " + word;
				if (estimateTextWidth(candidate, fontSize, font) <= maxWidth) {
					current.append(' ').append(word);
				} else {
					lines.add(current.toString());
					current.setLength(0);
					appendWord(lines, current, word, maxWidth, fontSize, font);
				}
			}

			if (current.length() > 0) {
				lines.add(current.toString());
			}
			return lines;
		}

		private void appendWord(
			List<String> lines,
			StringBuilder current,
			String word,
			float maxWidth,
			float fontSize,
			PdfFont font
		) {
			if (estimateTextWidth(word, fontSize, font) <= maxWidth) {
				current.append(word);
				return;
			}

			var remaining = word;
			while (!remaining.isEmpty()) {
				var split = findSplitIndex(remaining, maxWidth, fontSize, font);
				if (split >= remaining.length()) {
					current.append(remaining);
					return;
				}
				lines.add(remaining.substring(0, split) + "-");
				remaining = remaining.substring(split);
			}
		}

		private int findSplitIndex(String word, float maxWidth, float fontSize, PdfFont font) {
			for (int i = 1; i <= word.length(); i++) {
				var candidate = word.substring(0, i);
				var candidateWidth = estimateTextWidth(candidate + "-", fontSize, font);
				if (candidateWidth > maxWidth) {
					return Math.max(1, i - 1);
				}
			}
			return word.length();
		}

		private float estimateTextWidth(String text, float fontSize, PdfFont font) {
			float units = 0f;
			for (char caractere : text.toCharArray()) {
				units += estimateCharWidth(caractere, font);
			}
			return units * fontSize;
		}

		private float estimateCharWidth(char caractere, PdfFont font) {
			if (caractere == ' ') {
				return 0.28f;
			}
			if ("ilI.,;:'!|`".indexOf(caractere) >= 0) {
				return 0.23f;
			}
			if ("mwMW@#%&QO".indexOf(caractere) >= 0) {
				return font == PdfFont.BOLD ? 0.78f : 0.74f;
			}
			if (Character.isDigit(caractere)) {
				return 0.56f;
			}
			if (Character.isUpperCase(caractere)) {
				return font == PdfFont.BOLD ? 0.64f : 0.60f;
			}
			return font == PdfFont.BOLD ? 0.55f : 0.52f;
		}
	}
}
