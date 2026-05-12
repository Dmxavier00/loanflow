package com.api.loanflow.usuario.api.dto;

import com.api.loanflow.usuario.domain.Credor;

public record CredorLookupResponse(
	Long credorId,
	Long usuarioId,
	String nome,
	String email
) {
	public static CredorLookupResponse from(Credor credor) {
		return new CredorLookupResponse(
			credor.getId(),
			credor.getUsuario().getId(),
			credor.getUsuario().getNome(),
			credor.getUsuario().getEmail()
		);
	}
}
