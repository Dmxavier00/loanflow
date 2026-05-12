package com.api.loanflow.contrato.api.dto;

import jakarta.validation.constraints.AssertTrue;

/**
 * Payload usado para confirmar o aceite explicito do contrato pelo cliente.
 *
 * @param aceite deve ser {@code true} para confirmar a concordancia com o contrato
 */
public record AssinarContratoRequest(
	@AssertTrue(message = "O aceite explícito é obrigatório.") boolean aceite
) {
}
