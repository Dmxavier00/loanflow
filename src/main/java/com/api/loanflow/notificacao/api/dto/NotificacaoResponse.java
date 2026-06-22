package com.api.loanflow.notificacao.api.dto;

import com.api.loanflow.notificacao.dominio.Notificacao;
import com.api.loanflow.notificacao.dominio.TipoNotificacao;

import java.time.LocalDateTime;

public record NotificacaoResponse(
	Long id,
	TipoNotificacao tipo,
	String mensagem,
	LocalDateTime dataEnvio,
	boolean lida,
	String referenciaTipo,
	Long referenciaId
) {
	public static NotificacaoResponse from(Notificacao notificacao) {
		return new NotificacaoResponse(
			notificacao.getId(),
			notificacao.getTipo(),
			notificacao.getMensagem(),
			notificacao.getDataEnvio(),
			notificacao.isLida(),
			notificacao.getReferenciaTipo(),
			notificacao.getReferenciaId()
		);
	}
}
