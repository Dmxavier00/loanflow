package com.api.loanflow.contrato.dominio;

/**
 * Representa o ciclo de vida de um contrato dentro do fluxo de formalizacao.
 */
public enum ContratoStatus {
	/**
	 * Contrato gerado antes da formalizacao automatica final.
	 */
	GERADO,
	/**
	 * Contrato formalizado e apto para gerar ou acompanhar parcelas.
	 */
	FORMALIZADO,
	/**
	 * Contrato encerrado por pagamento integral das parcelas.
	 */
	QUITADO,
	/**
	 * Contrato encerrado sem formalizacao concluida.
	 */
	EXPIRADO,
	/**
	 * Contrato encerrado manualmente ou invalidado pelo negocio.
	 */
	CANCELADO
}
