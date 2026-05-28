package com.api.loanflow.usuario.api.dto;

import com.api.loanflow.usuario.dominio.EstadoCivil;
import com.api.loanflow.usuario.dominio.NivelRiscoCredito;
import com.api.loanflow.usuario.dominio.Role;
import com.api.loanflow.usuario.dominio.TipoDocumentoIdentidade;
import com.api.loanflow.usuario.dominio.Usuario;
import com.api.loanflow.usuario.dominio.UsuarioStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UsuarioResponse(
	Long id,
	String nome,
	String cpf,
	String email,
	EstadoCivil estadoCivil,
	String nacionalidade,
	String profissao,
	LocalDate dataNascimento,
	String telefone,
	TipoDocumentoIdentidade tipoDocumentoIdentidade,
	String documentoIdentidade,
	String orgaoEmissor,
	boolean pessoaExpostaPoliticamente,
	EnderecoResponse endereco,
	UsuarioStatus status,
	Role papel,
	LocalDateTime dataCadastro,
	Long solicitanteId,
	Long credorId,
	Long administradorId,
	Long contaBancariaId,
	BigDecimal rendaMensal,
	Integer scoreCredito,
	NivelRiscoCredito nivelRisco,
	BigDecimal saldoDisponivelSimulado,
	Integer limiteOperacoes
) {
	public static UsuarioResponse from(Usuario usuario) {
		return from(usuario, null, null, null, null);
	}

	public static UsuarioResponse from(Usuario usuario, Long solicitanteId, Long credorId, Long administradorId, Long contaBancariaId) {
		return from(usuario, solicitanteId, credorId, administradorId, contaBancariaId, null, null, null);
	}

	public static UsuarioResponse from(
		Usuario usuario,
		Long solicitanteId,
		Long credorId,
		Long administradorId,
		Long contaBancariaId,
		BigDecimal rendaMensal,
		Integer scoreCredito,
		NivelRiscoCredito nivelRisco,
		BigDecimal saldoDisponivelSimulado,
		Integer limiteOperacoes
	) {
		return new UsuarioResponse(
			usuario.getId(),
			usuario.getNome(),
			usuario.getCpf(),
			usuario.getEmail(),
			usuario.getEstadoCivil(),
			usuario.getNacionalidade(),
			usuario.getProfissao(),
			usuario.getDataNascimento(),
			usuario.getTelefone(),
			usuario.getTipoDocumentoIdentidade(),
			usuario.getDocumentoIdentidade(),
			usuario.getOrgaoEmissor(),
			usuario.isPessoaExpostaPoliticamente(),
			EnderecoResponse.from(usuario.getEndereco()),
			usuario.getStatus(),
			usuario.getPapel(),
			usuario.getDataCadastro(),
			solicitanteId,
			credorId,
			administradorId,
			contaBancariaId,
			rendaMensal,
			scoreCredito,
			nivelRisco,
			saldoDisponivelSimulado,
			limiteOperacoes
		);
	}

	public static UsuarioResponse from(
		Usuario usuario,
		Long solicitanteId,
		Long credorId,
		Long administradorId,
		Long contaBancariaId,
		BigDecimal rendaMensal,
		BigDecimal saldoDisponivelSimulado,
		Integer limiteOperacoes
	) {
		return from(usuario, solicitanteId, credorId, administradorId, contaBancariaId, rendaMensal, null, null, saldoDisponivelSimulado, limiteOperacoes);
	}
}
