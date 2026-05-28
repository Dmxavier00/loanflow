package com.api.loanflow.usuario.dominio;

public enum NivelRiscoCredito {
	BAIXO,
	MEDIO,
	ALTO;

	public static NivelRiscoCredito fromScore(Integer score) {
		if (score == null) {
			return null;
		}
		if (score >= 70) {
			return BAIXO;
		}
		if (score >= 40) {
			return MEDIO;
		}
		return ALTO;
	}
}
