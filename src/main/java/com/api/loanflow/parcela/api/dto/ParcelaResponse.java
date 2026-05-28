package com.api.loanflow.parcela.api.dto;

import com.api.loanflow.parcela.dominio.Parcela;
import com.api.loanflow.parcela.dominio.ParcelaStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaResponse(
	Long id,
	Long contratoId,
	String numeroContrato,
	String credorNome,
	Integer numero,
	BigDecimal valorPrevisto,
	BigDecimal valorPagoAcumulado,
	LocalDate dataVencimento,
	ParcelaStatus status
) {
	public static ParcelaResponse from(Parcela parcela) {
		return new ParcelaResponse(
			parcela.getId(),
			parcela.getContrato().getId(),
			parcela.getContrato().getNumeroContrato(),
			parcela.getContrato().getProposta().getCredor() == null
				? null
				: parcela.getContrato().getProposta().getCredor().getUsuario().getNome(),
			parcela.getNumero(),
			parcela.getValorPrevisto(),
			parcela.getValorPagoAcumulado(),
			parcela.getDataVencimento(),
			parcela.getStatus()
		);
	}
}
