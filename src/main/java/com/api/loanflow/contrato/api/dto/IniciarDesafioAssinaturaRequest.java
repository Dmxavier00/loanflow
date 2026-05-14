package com.api.loanflow.contrato.api.dto;

import com.api.loanflow.contrato.dominio.MetodoAutenticacaoAssinatura;
import jakarta.validation.constraints.NotNull;

public record IniciarDesafioAssinaturaRequest(
	@NotNull(message = "Informe o metodo de autenticacao para iniciar a assinatura.")
	MetodoAutenticacaoAssinatura metodo
) {
}
