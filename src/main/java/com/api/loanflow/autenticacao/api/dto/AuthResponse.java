package com.api.loanflow.autenticacao.api.dto;

import com.api.loanflow.usuario.api.dto.UsuarioResponse;

public record AuthResponse(
	String tokenType,
	String accessToken,
	long expiresInSeconds,
	UsuarioResponse usuario
) {
}
