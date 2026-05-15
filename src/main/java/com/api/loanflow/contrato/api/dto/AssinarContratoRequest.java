package com.api.loanflow.contrato.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssinarContratoRequest(
	@AssertTrue(message = "O aceite explícito é obrigatório.") boolean aceite,
	@NotNull(message = "O desafio de assinatura é obrigatório.") UUID desafioId,
	@NotBlank(message = "Informe o código ou a senha para confirmar a assinatura.")
	@Size(min = 4, max = 80, message = "O código ou a senha informados estão fora do tamanho esperado.")
	String codigo
) {
}
