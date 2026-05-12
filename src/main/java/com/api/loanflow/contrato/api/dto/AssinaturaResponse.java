package com.api.loanflow.contrato.api.dto;

import com.api.loanflow.contrato.domain.AssinaturaEletronica;
import com.api.loanflow.contrato.domain.TipoAceite;
import com.api.loanflow.usuario.domain.Role;

import java.time.LocalDateTime;

public record AssinaturaResponse(
	Long id,
	Long contratoId,
	Long usuarioId,
	Role papelSignatario,
	TipoAceite tipoAceite,
	String hashAssinatura,
	LocalDateTime registroTemporal,
	boolean valida
) {
	public static AssinaturaResponse from(AssinaturaEletronica assinatura) {
		return new AssinaturaResponse(
			assinatura.getId(),
			assinatura.getContrato().getId(),
			assinatura.getUsuario().getId(),
			assinatura.getPapelSignatario(),
			assinatura.getTipoAceite(),
			assinatura.getHashAssinatura(),
			assinatura.getRegistroTemporal(),
			assinatura.isValida()
		);
	}
}
