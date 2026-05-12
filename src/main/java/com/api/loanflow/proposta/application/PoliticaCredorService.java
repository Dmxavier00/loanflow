package com.api.loanflow.proposta.application;

import com.api.loanflow.proposta.domain.Proposta;
import com.api.loanflow.proposta.domain.PropostaStatus;
import com.api.loanflow.proposta.infrastructure.persistence.PropostaRepository;
import com.api.loanflow.shared.exception.RegraNegocioException;
import com.api.loanflow.usuario.domain.Credor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class PoliticaCredorService {
	private static final List<PropostaStatus> STATUS_OPERACOES_ATIVAS = List.of(
		PropostaStatus.ACEITA,
		PropostaStatus.EM_ANALISE,
		PropostaStatus.APROVADA,
		PropostaStatus.CONTRATADA
	);

	private final PropostaRepository propostaRepository;

	public PoliticaCredorService(PropostaRepository propostaRepository) {
		this.propostaRepository = propostaRepository;
	}

	@Transactional(readOnly = true)
	public void validarDisponibilidade(Credor credor, Proposta proposta) {
		exigirSaldoDisponivelValido(credor);
		exigirLimiteOperacoesValido(credor);
		exigirLimiteOperacoesDisponivel(credor, proposta);
		exigirSaldoSuficiente(credor, proposta);
	}

	private void exigirSaldoDisponivelValido(Credor credor) {
		if (credor.getSaldoDisponivelSimulado() == null || credor.getSaldoDisponivelSimulado().signum() <= 0) {
			throw new RegraNegocioException("Credor precisa informar saldo disponível válido antes de assumir operações.");
		}
	}

	private void exigirLimiteOperacoesValido(Credor credor) {
		if (credor.getLimiteOperacoes() == null || credor.getLimiteOperacoes() <= 0) {
			throw new RegraNegocioException("Credor precisa informar limite de operações válido antes de assumir operações.");
		}
	}

	private void exigirLimiteOperacoesDisponivel(Credor credor, Proposta proposta) {
		long operacoesAtivas = propostaRepository.countByCredorUsuarioIdAndStatusIn(
			credor.getUsuario().getId(),
			STATUS_OPERACOES_ATIVAS
		);
		if (propostaJaContaComoAtivaParaOCredor(credor, proposta)) {
			operacoesAtivas = Math.max(0, operacoesAtivas - 1);
		}
		if (operacoesAtivas >= credor.getLimiteOperacoes()) {
			throw new RegraNegocioException("Credor atingiu o limite máximo de operações ativas.");
		}
	}

	private void exigirSaldoSuficiente(Credor credor, Proposta proposta) {
		var valorComprometido = propostaRepository.findByCredorUsuarioIdAndStatusInOrderByDataCriacaoDesc(
			credor.getUsuario().getId(),
			STATUS_OPERACOES_ATIVAS
		).stream()
			.filter(propostaAtiva -> !mesmaProposta(propostaAtiva, proposta))
			.map(Proposta::getValorSolicitado)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		var saldoLivre = credor.getSaldoDisponivelSimulado().subtract(valorComprometido);
		if (proposta.getValorSolicitado() == null || saldoLivre.compareTo(proposta.getValorSolicitado()) < 0) {
			throw new RegraNegocioException("Credor não possui saldo disponível suficiente para esta proposta.");
		}
	}

	private boolean propostaJaContaComoAtivaParaOCredor(Credor credor, Proposta proposta) {
		return proposta.getCredor() != null
			&& proposta.getCredor().getUsuario() != null
			&& Objects.equals(proposta.getCredor().getUsuario().getId(), credor.getUsuario().getId())
			&& STATUS_OPERACOES_ATIVAS.contains(proposta.getStatus());
	}

	private boolean mesmaProposta(Proposta propostaAtiva, Proposta propostaAtual) {
		return propostaAtiva.getId() != null
			&& propostaAtual.getId() != null
			&& Objects.equals(propostaAtiva.getId(), propostaAtual.getId());
	}
}
