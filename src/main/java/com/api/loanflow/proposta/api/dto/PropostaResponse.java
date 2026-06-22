package com.api.loanflow.proposta.api.dto;

import com.api.loanflow.compartilhado.financeiro.SimulacaoFinanceira;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import com.api.loanflow.proposta.dominio.Proposta;
import com.api.loanflow.proposta.dominio.PropostaStatus;
import com.api.loanflow.usuario.dominio.NivelRiscoCredito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PropostaResponse(
	Long id,
	String numeroProposta,
	Long solicitanteId,
	String solicitanteNome,
	Integer solicitanteScoreCredito,
	NivelRiscoCredito solicitanteNivelRisco,
	Long credorId,
	BigDecimal valorSolicitado,
	BigDecimal taxaJuros,
	Integer prazoMeses,
	BigDecimal valorTotalComJuros,
	String finalidade,
	CategoriaFinalidade categoriaFinalidade,
	String descricaoDetalhada,
	PropostaStatus status,
	LocalDateTime dataCriacao,
	LocalDate dataExpiracao,
	Long contratoId,
	String numeroContrato,
	ContratoStatus contratoStatus
) {
	public PropostaResponse(
		Long id,
		String numeroProposta,
		Long solicitanteId,
		String solicitanteNome,
		Integer solicitanteScoreCredito,
		NivelRiscoCredito solicitanteNivelRisco,
		Long credorId,
		BigDecimal valorSolicitado,
		BigDecimal taxaJuros,
		Integer prazoMeses,
		BigDecimal valorTotalComJuros,
		String finalidade,
		CategoriaFinalidade categoriaFinalidade,
		String descricaoDetalhada,
		PropostaStatus status,
		LocalDateTime dataCriacao,
		LocalDate dataExpiracao
	) {
		this(
			id,
			numeroProposta,
			solicitanteId,
			solicitanteNome,
			solicitanteScoreCredito,
			solicitanteNivelRisco,
			credorId,
			valorSolicitado,
			taxaJuros,
			prazoMeses,
			valorTotalComJuros,
			finalidade,
			categoriaFinalidade,
			descricaoDetalhada,
			status,
			dataCriacao,
			dataExpiracao,
			null,
			null,
			null
		);
	}

	public static PropostaResponse from(Proposta proposta) {
		var contrato = proposta.getContrato();
		return new PropostaResponse(
			proposta.getId(),
			proposta.getNumeroProposta(),
			proposta.getSolicitante().getId(),
			proposta.getSolicitante().getUsuario().getNome(),
			proposta.getSolicitante().getScoreCreditoSimulado(),
			proposta.getSolicitante().getNivelRisco(),
			proposta.getCredor() == null ? null : proposta.getCredor().getId(),
			proposta.getValorSolicitado(),
			proposta.getTaxaJuros(),
			proposta.getPrazoMeses(),
			SimulacaoFinanceira.calcularTotalComJuros(proposta.getValorSolicitado(), proposta.getTaxaJuros()),
			proposta.getFinalidade(),
			proposta.getCategoriaFinalidade(),
			proposta.getDescricaoDetalhada(),
			proposta.getStatus(),
			proposta.getDataCriacao(),
			proposta.getDataExpiracao(),
			contrato == null ? null : contrato.getId(),
			contrato == null ? null : contrato.getNumeroContrato(),
			contrato == null ? null : contrato.getStatus()
		);
	}
}
