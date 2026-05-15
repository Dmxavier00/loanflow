package com.api.loanflow.contrato.api.dto;

import com.api.loanflow.contrato.dominio.DesafioAssinatura;
import com.api.loanflow.contrato.dominio.MetodoAutenticacaoAssinatura;

import java.time.LocalDateTime;
import java.util.UUID;

public record IniciarDesafioAssinaturaResponse(
	UUID desafioId,
	MetodoAutenticacaoAssinatura metodo,
	LocalDateTime expiraEm,
	String mascaraDestino,
	String mensagem
) {
	public static IniciarDesafioAssinaturaResponse from(DesafioAssinatura desafio, String mascaraDestino) {
		return new IniciarDesafioAssinaturaResponse(
			desafio.getId(),
			desafio.getMetodoAutenticacao(),
			desafio.getExpiraEm(),
			mascaraDestino,
			montarMensagem(desafio.getMetodoAutenticacao(), mascaraDestino)
		);
	}

	private static String montarMensagem(MetodoAutenticacaoAssinatura metodo, String mascaraDestino) {
		if (metodo == MetodoAutenticacaoAssinatura.CODIGO_ONE_TIME && mascaraDestino != null && !mascaraDestino.isBlank()) {
			return "Código temporário emitido para o destino mascarado. Confirme a assinatura antes do prazo informado.";
		}
		if (metodo == MetodoAutenticacaoAssinatura.CODIGO_ONE_TIME) {
			return "Código temporário emitido. Confirme a assinatura antes do prazo informado.";
		}
		return "Desafio iniciado. Confirme a assinatura antes do prazo informado.";
	}
}
