package com.api.loanflow.autenticacao.api.dto;

import com.api.loanflow.usuario.api.dto.AtualizarContaBancariaRequest;
import com.api.loanflow.usuario.api.dto.EnderecoRequest;
import com.api.loanflow.usuario.dominio.EstadoCivil;
import com.api.loanflow.usuario.dominio.Role;
import com.api.loanflow.usuario.dominio.TipoDocumentoIdentidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterRequest(
	@NotBlank @Size(max = 120) String nome,
	@NotBlank @Size(max = 14) String cpf,
	@NotBlank @Email @Size(max = 160) String email,
	@NotNull EstadoCivil estadoCivil,
	@NotBlank @Size(max = 60) String nacionalidade,
	@NotBlank @Size(max = 120) String profissao,
	@NotNull @Past LocalDate dataNascimento,
	@NotBlank @Size(max = 20) String telefone,
	@NotNull TipoDocumentoIdentidade tipoDocumentoIdentidade,
	@NotBlank @Size(max = 40) String documentoIdentidade,
	@NotBlank @Size(max = 20) String orgaoEmissor,
	@NotNull Boolean pessoaExpostaPoliticamente,
	@NotNull @Valid EnderecoRequest endereco,
	@NotBlank @Size(min = 8, max = 80) String senha,
	@NotNull Role papel,
	@DecimalMin(value = "0.00", inclusive = true) BigDecimal rendaMensal,
	@Size(max = 80) String tipoOcupacao,
	@DecimalMin(value = "0.00", inclusive = true) BigDecimal saldoDisponivelSimulado,
	@Valid AtualizarContaBancariaRequest contaBancaria
) {
}
