package com.api.loanflow.usuario.api.dto;

import com.api.loanflow.usuario.domain.EstadoCivil;
import com.api.loanflow.usuario.domain.TipoDocumentoIdentidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtualizarPerfilRequest(
	@NotBlank @Size(max = 120) String nome,
	@NotNull EstadoCivil estadoCivil,
	@NotBlank @Size(max = 60) String nacionalidade,
	@NotBlank @Size(max = 120) String profissao,
	@NotNull @Past LocalDate dataNascimento,
	@NotBlank @Size(max = 20) String telefone,
	@NotNull TipoDocumentoIdentidade tipoDocumentoIdentidade,
	@NotBlank @Size(max = 40) String documentoIdentidade,
	@NotBlank @Size(max = 20) String orgaoEmissor,
	@NotNull Boolean pessoaExpostaPoliticamente,
	@NotNull @Valid EnderecoRequest endereco
) {
}
