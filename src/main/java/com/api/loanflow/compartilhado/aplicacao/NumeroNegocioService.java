package com.api.loanflow.compartilhado.aplicacao;

import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.proposta.dominio.Proposta;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class NumeroNegocioService {
	private static final Pattern PROPOSTA_ATUAL_PATTERN = Pattern.compile("^PPT-\\d{4}-\\d{6,}$");
	private static final Pattern CONTRATO_ATUAL_PATTERN = Pattern.compile("^CTR-\\d{4}-\\d{6,}$");

	public String gerarNumeroTemporario() {
		return "TMP-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
	}

	public String gerarNumeroProposta(Proposta proposta) {
		return "PPT-%d-%06d".formatted(resolverAno(proposta.getDataCriacao()), proposta.getId());
	}

	public String gerarNumeroContrato(Contrato contrato) {
		return "CTR-%d-%06d".formatted(resolverAno(contrato.getDataGeracao()), contrato.getId());
	}

	public boolean numeroPropostaEstaNoFormatoAtual(String numeroProposta) {
		return numeroProposta != null && PROPOSTA_ATUAL_PATTERN.matcher(numeroProposta).matches();
	}

	public boolean numeroContratoEstaNoFormatoAtual(String numeroContrato) {
		return numeroContrato != null && CONTRATO_ATUAL_PATTERN.matcher(numeroContrato).matches();
	}

	private int resolverAno(LocalDateTime dataReferencia) {
		return (dataReferencia == null ? LocalDateTime.now() : dataReferencia).getYear();
	}
}
