package com.api.loanflow.proposta.api.dto;

import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import com.api.loanflow.proposta.dominio.Proposta;
import com.api.loanflow.proposta.dominio.PropostaStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PropostaResponse(
	Long id,
	Long solicitanteId,
	String solicitanteNome,
	Long credorId,
	BigDecimal valorSolicitado,
	BigDecimal taxaJuros,
	Integer prazoMeses,
	String finalidade,
	CategoriaFinalidade categoriaFinalidade,
	String descricaoDetalhada,
	PropostaStatus status,
	LocalDateTime dataCriacao,
	LocalDate dataExpiracao
) {
	public static PropostaResponse from(Proposta proposta) {
		return new PropostaResponse(
			proposta.getId(),
			proposta.getSolicitante().getId(),
			proposta.getSolicitante().getUsuario().getNome(),
			proposta.getCredor() == null ? null : proposta.getCredor().getId(),
			proposta.getValorSolicitado(),
			proposta.getTaxaJuros(),
			proposta.getPrazoMeses(),
			proposta.getFinalidade(),
			proposta.getCategoriaFinalidade(),
			proposta.getDescricaoDetalhada(),
			proposta.getStatus(),
			proposta.getDataCriacao(),
			proposta.getDataExpiracao()
		);
	}
}
