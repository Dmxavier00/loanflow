package com.api.loanflow.proposta.api.dto;

import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AtualizarPropostaRequest(
	@NotNull @DecimalMin(value = "0.01") BigDecimal valorSolicitado,
	@NotNull
	@DecimalMin(value = "5.00", message = "Taxa de juros deve estar entre 5% e 25%.")
	@DecimalMax(value = "25.00", message = "Taxa de juros deve estar entre 5% e 25%.")
	BigDecimal taxaJuros,
	@NotNull @Min(1) @Max(value = 12, message = "Prazo deve ser de no máximo 12 meses.") Integer prazoMeses,
	@NotBlank @Size(max = 160) String finalidade,
	@NotNull CategoriaFinalidade categoriaFinalidade,
	@NotBlank @Size(max = 2000) String descricaoDetalhada
) {
}
