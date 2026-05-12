package com.api.loanflow.usuario.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
	@NotBlank @Size(min = 8, max = 80) String senha
) {
}
