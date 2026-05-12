package com.api.loanflow.usuario.api.dto;

import com.api.loanflow.usuario.domain.Endereco;

public record EnderecoResponse(
	String cep,
	String logradouro,
	String numero,
	String complemento,
	String bairro,
	String cidade,
	String uf
) {
	public static EnderecoResponse from(Endereco endereco) {
		if (endereco == null || !endereco.isInformado()) {
			return null;
		}

		return new EnderecoResponse(
			endereco.getCep(),
			endereco.getLogradouro(),
			endereco.getNumero(),
			endereco.getComplemento(),
			endereco.getBairro(),
			endereco.getCidade(),
			endereco.getUf()
		);
	}
}
