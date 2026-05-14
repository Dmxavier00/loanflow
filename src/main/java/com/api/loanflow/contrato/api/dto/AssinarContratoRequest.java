package com.api.loanflow.contrato.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssinarContratoRequest(
	@AssertTrue(message = "O aceite explicito e obrigatorio.") boolean aceite,
	@NotNull(message = "O desafio de assinatura e obrigatorio.") UUID desafioId,
	@NotBlank(message = "Informe o codigo ou a senha para confirmar a assinatura.")
	@Size(min = 4, max = 80, message = "O codigo ou a senha informados estao fora do tamanho esperado.")
	String codigo
) {
}
