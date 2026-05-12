package com.api.loanflow.usuario.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record AtualizarDadosFinanceirosRequest(
	@DecimalMin(value = "0.01", inclusive = true) BigDecimal rendaMensal,
	@DecimalMin(value = "0.01", inclusive = true) BigDecimal saldoDisponivelSimulado,
	@Min(1) Integer limiteOperacoes
) {
}
