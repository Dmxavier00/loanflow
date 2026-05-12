package com.api.loanflow.contrato.domain;

/**
 * Representa o ciclo de vida de um contrato dentro do fluxo de formalizacao.
 */
public enum ContratoStatus {
	/**
	 * Contrato gerado, mas ainda nao liberado para coleta de assinaturas.
	 */
	GERADO,
	/**
	 * Contrato disponivel e aguardando as assinaturas obrigatorias.
	 */
	AGUARDANDO_ASSINATURAS,
	/**
	 * Pelo menos um signatario assinou, mas ainda faltam aceite(s).
	 */
	ASSINADO_PARCIALMENTE,
	/**
	 * Todas as assinaturas foram concluidas e o contrato foi formalizado.
	 */
	FORMALIZADO,
	/**
	 * Prazo de assinatura encerrado sem conclusao da formalizacao.
	 */
	EXPIRADO,
	/**
	 * Contrato encerrado manualmente ou invalidado pelo negocio.
	 */
	CANCELADO
}
