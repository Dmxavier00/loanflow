package com.api.loanflow.contrato.api.dto;

import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;

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
	LocalDateTime dataFormalizacao
) {
	public static ContratoResponse from(Contrato contrato) {
		return new ContratoResponse(
			contrato.getId(),
			contrato.getProposta().getId(),
			contrato.getNumeroContrato(),
			contrato.getProposta().getFinalidade(),
			contrato.getStatus(),
			contrato.getHashDocumento(),
			contrato.getPdfPath(),
			contrato.getDataGeracao(),
			contrato.getDataFormalizacao()
		);
	}
}
