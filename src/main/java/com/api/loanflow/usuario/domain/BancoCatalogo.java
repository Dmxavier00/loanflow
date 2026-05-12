package com.api.loanflow.usuario.domain;

import java.util.Set;

public final class BancoCatalogo {
	private static final Set<String> BANCOS_SUPORTADOS = Set.of(
		"Banco do Brasil",
		"Bradesco",
		"Caixa Econômica Federal",
		"Itaú Unibanco",
		"Santander",
		"Nubank",
		"Inter",
		"C6 Bank",
		"BTG Pactual",
		"Sicredi",
		"Sicoob",
		"Banco Safra",
		"PagBank",
		"Mercado Pago"
	);

	private BancoCatalogo() {
	}

	public static boolean contem(String banco) {
		return BANCOS_SUPORTADOS.contains(banco);
	}
}
