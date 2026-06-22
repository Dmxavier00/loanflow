package com.api.loanflow.administracao.api.dto;

public record AdminDashboardResponse(
	long totalUsuarios,
	long totalPropostas,
	long propostasAprovadas,
	long propostasContratadas,
	long contratosFormalizados,
	long parcelasAbertas,
	long parcelasEmAtraso,
	long pagamentosRegistrados,
	long auditoriasRegistradas
) {
}
