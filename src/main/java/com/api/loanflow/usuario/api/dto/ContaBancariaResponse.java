package com.api.loanflow.usuario.api.dto;

import com.api.loanflow.usuario.dominio.ContaBancaria;
import com.api.loanflow.usuario.dominio.TipoContaBancaria;

import java.time.LocalDateTime;

public record ContaBancariaResponse(
	Long id,
	Long usuarioId,
	String banco,
	String agencia,
	String numeroConta,
	TipoContaBancaria tipoConta,
	String chavePix,
	LocalDateTime dataCadastro,
	LocalDateTime dataAtualizacao
) {
	public static ContaBancariaResponse from(ContaBancaria contaBancaria) {
		return new ContaBancariaResponse(
			contaBancaria.getId(),
			contaBancaria.getUsuario().getId(),
			contaBancaria.getBanco(),
			contaBancaria.getAgencia(),
			contaBancaria.getNumeroConta(),
			contaBancaria.getTipoConta(),
			contaBancaria.getChavePix(),
			contaBancaria.getDataCadastro(),
			contaBancaria.getDataAtualizacao()
		);
	}
}
