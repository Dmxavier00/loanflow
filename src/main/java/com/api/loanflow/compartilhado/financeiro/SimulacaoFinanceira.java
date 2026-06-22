package com.api.loanflow.compartilhado.financeiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SimulacaoFinanceira {
	private static final BigDecimal CEM = BigDecimal.valueOf(100);

	private SimulacaoFinanceira() {
	}

	public static BigDecimal calcularTotalComJuros(BigDecimal valorPrincipal, BigDecimal taxaJuros) {
		if (valorPrincipal == null || taxaJuros == null) {
			return null;
		}

		var fatorJuros = BigDecimal.ONE.add(taxaJuros.divide(CEM, 8, RoundingMode.HALF_UP));
		return valorPrincipal.multiply(fatorJuros).setScale(2, RoundingMode.HALF_UP);
	}

	public static BigDecimal calcularParcelaMedia(BigDecimal valorTotal, Integer prazoMeses) {
		if (valorTotal == null || prazoMeses == null || prazoMeses <= 0) {
			return null;
		}

		return valorTotal.divide(BigDecimal.valueOf(prazoMeses), 2, RoundingMode.HALF_UP);
	}
}
