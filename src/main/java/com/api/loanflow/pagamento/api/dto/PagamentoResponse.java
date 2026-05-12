package com.api.loanflow.pagamento.api.dto;

import com.api.loanflow.pagamento.domain.FormaPagamento;
import com.api.loanflow.pagamento.domain.Pagamento;
import com.api.loanflow.pagamento.domain.PagamentoStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponse(
	Long id,
	Long parcelaId,
	Long usuarioRegistradorId,
	BigDecimal valorPago,
	LocalDateTime dataHoraPagamento,
	FormaPagamento formaPagamento,
	PagamentoStatus status,
	String comprovante
) {
	public static PagamentoResponse from(Pagamento pagamento) {
		return new PagamentoResponse(
			pagamento.getId(),
			pagamento.getParcela().getId(),
			pagamento.getUsuarioRegistrador().getId(),
			pagamento.getValorPago(),
			pagamento.getDataHoraPagamento(),
			pagamento.getFormaPagamento(),
			pagamento.getStatus(),
			pagamento.getComprovante()
		);
	}
}
