package com.api.loanflow.proposta.application;

import com.api.loanflow.contrato.domain.ContratoStatus;
import com.api.loanflow.parcela.domain.Parcela;
import com.api.loanflow.parcela.domain.ParcelaStatus;
import com.api.loanflow.parcela.infrastructure.persistence.ParcelaRepository;
import com.api.loanflow.shared.exception.RegraNegocioException;
import com.api.loanflow.usuario.domain.SolicitanteCredito;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PoliticaCreditoService {
	private static final int LIMITE_CONTRATOS_ATIVOS = 2;
	private static final BigDecimal LIMITE_COMPROMETIMENTO_RENDA = new BigDecimal("0.30");

	private final ParcelaRepository parcelaRepository;

	public PoliticaCreditoService(ParcelaRepository parcelaRepository) {
		this.parcelaRepository = parcelaRepository;
	}

	@Transactional(readOnly = true)
	public void validarNovaContratacao(
		SolicitanteCredito solicitante,
		BigDecimal valorSolicitado,
		BigDecimal taxaJuros,
		Integer prazoMeses
	) {
		exigirRendaMensalValida(solicitante);
		exigirSemInadimplencia(solicitante.getUsuario().getId());
		exigirLimiteDeContratosAtivos(solicitante.getUsuario().getId());
		exigirComprometimentoDeRenda(solicitante, valorSolicitado, taxaJuros, prazoMeses);
	}

	private void exigirRendaMensalValida(SolicitanteCredito solicitante) {
		if (solicitante.getRendaMensal() == null || solicitante.getRendaMensal().signum() <= 0) {
			throw new RegraNegocioException("Solicitante precisa informar renda mensal válida antes de contratar empréstimo.");
		}
	}

	private void exigirSemInadimplencia(Long usuarioId) {
		var parcelasEmAtraso = EnumSet.of(ParcelaStatus.VENCIDA, ParcelaStatus.EM_ATRASO);
		var possuiAtrasoMarcado = parcelaRepository.existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusIn(
			ContratoStatus.FORMALIZADO,
			usuarioId,
			parcelasEmAtraso
		);
		var possuiParcelaVencidaNaoPaga = parcelaRepository.existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNotAndDataVencimentoBefore(
			ContratoStatus.FORMALIZADO,
			usuarioId,
			ParcelaStatus.PAGA,
			LocalDate.now()
		);
		if (possuiAtrasoMarcado || possuiParcelaVencidaNaoPaga) {
			throw new RegraNegocioException("Solicitante possui parcelas vencidas ou em atraso e não pode contratar novo empréstimo.");
		}
	}

	private void exigirLimiteDeContratosAtivos(Long usuarioId) {
		var contratosAtivos = parcelaRepository.countDistinctContratoIdByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNot(
			ContratoStatus.FORMALIZADO,
			usuarioId,
			ParcelaStatus.PAGA
		);
		if (contratosAtivos >= LIMITE_CONTRATOS_ATIVOS) {
			throw new RegraNegocioException("Solicitante atingiu o limite de 2 contratos ativos simultâneos.");
		}
	}

	private void exigirComprometimentoDeRenda(
		SolicitanteCredito solicitante,
		BigDecimal valorSolicitado,
		BigDecimal taxaJuros,
		Integer prazoMeses
	) {
		var rendaMensal = solicitante.getRendaMensal();
		var comprometimentoAtual = calcularComprometimentoAtual(solicitante.getUsuario().getId());
		var parcelaSimulada = calcularParcelaMensalSimulada(valorSolicitado, taxaJuros, prazoMeses);
		var comprometimentoTotal = comprometimentoAtual.add(parcelaSimulada);
		var limitePermitido = rendaMensal.multiply(LIMITE_COMPROMETIMENTO_RENDA).setScale(2, RoundingMode.HALF_UP);
		if (comprometimentoTotal.compareTo(limitePermitido) > 0) {
			throw new RegraNegocioException("Comprometimento mensal excede 30% da renda informada para o solicitante.");
		}
	}

	private BigDecimal calcularComprometimentoAtual(Long usuarioId) {
		var parcelasPendentes = parcelaRepository.findByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNotOrderByContratoIdAscNumeroAsc(
			ContratoStatus.FORMALIZADO,
			usuarioId,
			ParcelaStatus.PAGA
		);
		Map<Long, Parcela> primeiraParcelaPendentePorContrato = new LinkedHashMap<>();
		for (var parcela : parcelasPendentes) {
			primeiraParcelaPendentePorContrato.putIfAbsent(parcela.getContrato().getId(), parcela);
		}
		return primeiraParcelaPendentePorContrato.values().stream()
			.map(this::calcularSaldoPendente)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal calcularSaldoPendente(Parcela parcela) {
		var saldo = parcela.getValorPrevisto().subtract(parcela.getValorPagoAcumulado());
		return saldo.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal calcularParcelaMensalSimulada(BigDecimal valorSolicitado, BigDecimal taxaJuros, Integer prazoMeses) {
		var fatorJuros = BigDecimal.ONE.add(taxaJuros.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
		var total = valorSolicitado.multiply(fatorJuros).setScale(2, RoundingMode.HALF_UP);
		return total.divide(BigDecimal.valueOf(prazoMeses), 2, RoundingMode.HALF_UP);
	}
}
