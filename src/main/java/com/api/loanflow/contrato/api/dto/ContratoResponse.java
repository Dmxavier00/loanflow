package com.api.loanflow.contrato.api.dto;

import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;

import java.nio.file.Path;
import java.time.LocalDateTime;

public record ContratoResponse(
	Long id,
	Long propostaId,
	String numeroContrato,
	String finalidade,
	ContratoStatus status,
	String hashDocumento,
	String pdfPath,
	LocalDateTime dataGeracao,
	LocalDateTime dataExpiracaoAssinatura,
	LocalDateTime dataFormalizacao,
	String motivoExpiracao
) {
	public static ContratoResponse from(Contrato contrato) {
		return new ContratoResponse(
			contrato.getId(),
			contrato.getProposta().getId(),
			contrato.getNumeroContrato(),
			contrato.getProposta().getFinalidade(),
			contrato.getStatus(),
			contrato.getHashDocumento(),
			extractPdfFileName(contrato.getPdfPath()),
			contrato.getDataGeracao(),
			contrato.getDataExpiracaoAssinatura(),
			contrato.getDataFormalizacao(),
			contrato.getMotivoExpiracao()
		);
	}

	private static String extractPdfFileName(String pdfPath) {
		if (pdfPath == null || pdfPath.isBlank()) {
			return pdfPath;
		}

		try {
			var fileName = Path.of(pdfPath).getFileName();
			return fileName != null ? fileName.toString() : pdfPath;
		} catch (RuntimeException exception) {
			return pdfPath;
		}
	}
}
