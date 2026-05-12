package com.api.loanflow.auditoria.api.dto;

import com.api.loanflow.auditoria.dominio.Auditoria;
import com.api.loanflow.auditoria.dominio.AuditoriaAcao;

import java.time.LocalDateTime;

public record AuditoriaResponse(
	Long id,
	Long usuarioId,
	AuditoriaAcao acao,
	String entidadeTipo,
	Long entidadeId,
	LocalDateTime dataHora,
	String ipOrigem,
	String detalhes
) {
	public static AuditoriaResponse from(Auditoria auditoria) {
		return new AuditoriaResponse(
			auditoria.getId(),
			auditoria.getUsuario() == null ? null : auditoria.getUsuario().getId(),
			auditoria.getAcao(),
			auditoria.getEntidadeTipo(),
			auditoria.getEntidadeId(),
			auditoria.getDataHora(),
			auditoria.getIpOrigem(),
			auditoria.getDetalhes()
		);
	}
}
