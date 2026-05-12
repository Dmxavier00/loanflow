package com.api.loanflow.shared.cep.api.dto;

public record CepLookupResponse(
	String cep,
	String logradouro,
	String bairro,
	String cidade,
	String uf
) {
}
