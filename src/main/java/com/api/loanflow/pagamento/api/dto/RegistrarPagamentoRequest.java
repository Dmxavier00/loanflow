package com.api.loanflow.pagamento.api.dto;

import com.api.loanflow.pagamento.dominio.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegistrarPagamentoRequest(
	@NotNull @DecimalMin("0.01") BigDecimal valorPago,
	@NotNull FormaPagamento formaPagamento,
	@Size(max = 500) String comprovante
) {
}
