package com.api.loanflow.contrato.api.dto;

import com.api.loanflow.compartilhado.financeiro.SimulacaoFinanceira;
import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.usuario.dominio.ContaBancaria;
import com.api.loanflow.usuario.dominio.NivelRiscoCredito;
import com.api.loanflow.usuario.dominio.UsuarioStatus;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

public record ContratoResponse(
	Long id,
	Long propostaId,
	String numeroProposta,
	String numeroContrato,
	String finalidade,
	Long solicitanteId,
	String solicitanteNome,
	String solicitanteCpf,
	String solicitanteEmail,
	String solicitanteTelefone,
	BigDecimal solicitanteRendaMensal,
	Integer solicitanteScoreCredito,
	NivelRiscoCredito solicitanteNivelRisco,
	Long credorId,
	String credorNome,
	String credorCpf,
	String credorEmail,
	String credorTelefone,
	String credorBanco,
	String credorChavePix,
	UsuarioStatus credorStatus,
	BigDecimal credorSaldoDisponivelSimulado,
	BigDecimal credorTotalEmprestadoSimulado,
	Integer credorLimiteOperacoes,
	BigDecimal valorTotalComJuros,
	ContratoStatus status,
	String hashDocumento,
	String pdfPath,
	LocalDateTime dataGeracao,
	LocalDateTime dataFormalizacao
) {
	public static ContratoResponse from(Contrato contrato) {
		return from(contrato, null);
	}

	public static ContratoResponse from(Contrato contrato, ContaBancaria credorContaBancaria) {
		var proposta = contrato.getProposta();
		var solicitante = proposta.getSolicitante();
		var solicitanteUsuario = solicitante.getUsuario();
		var credor = proposta.getCredor();
		var credorUsuario = credor == null ? null : credor.getUsuario();

		return new ContratoResponse(
			contrato.getId(),
			proposta.getId(),
			proposta.getNumeroProposta(),
			contrato.getNumeroContrato(),
			proposta.getFinalidade(),
			solicitante.getId(),
			solicitanteUsuario.getNome(),
			solicitanteUsuario.getCpf(),
			solicitanteUsuario.getEmail(),
			solicitanteUsuario.getTelefone(),
			solicitante.getRendaMensal(),
			solicitante.getScoreCreditoSimulado(),
			solicitante.getNivelRisco(),
			credor == null ? null : credor.getId(),
			credorUsuario == null ? null : credorUsuario.getNome(),
			credorUsuario == null ? null : credorUsuario.getCpf(),
			credorUsuario == null ? null : credorUsuario.getEmail(),
			credorUsuario == null ? null : credorUsuario.getTelefone(),
			credorContaBancaria == null ? null : credorContaBancaria.getBanco(),
			credorContaBancaria == null ? null : credorContaBancaria.getChavePix(),
			credorUsuario == null ? null : credorUsuario.getStatus(),
			credor == null ? null : credor.getSaldoDisponivelSimulado(),
			credor == null ? null : credor.getTotalEmprestadoSimulado(),
			credor == null ? null : credor.getLimiteOperacoes(),
			SimulacaoFinanceira.calcularTotalComJuros(
				proposta.getValorSolicitado(),
				proposta.getTaxaJuros()
			),
			contrato.getStatus(),
			contrato.getHashDocumento(),
			extractPdfFileName(contrato.getPdfPath()),
			contrato.getDataGeracao(),
			contrato.getDataFormalizacao()
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
