package com.api.loanflow.usuario.api.dto;

import com.api.loanflow.usuario.domain.TipoContaBancaria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarContaBancariaRequest(
	@NotBlank @Size(max = 120) String banco,
	@NotBlank @Size(max = 20) String agencia,
	@NotBlank @Size(max = 30) String numeroConta,
	@NotNull TipoContaBancaria tipoConta,
	@Size(max = 120) String chavePix
) {
}
