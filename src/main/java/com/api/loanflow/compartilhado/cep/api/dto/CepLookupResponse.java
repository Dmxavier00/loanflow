package com.api.loanflow.compartilhado.cep.api.dto;

public record CepLookupResponse(
	String cep,
	String logradouro,
	String bairro,
	String cidade,
	String uf
) {
}
