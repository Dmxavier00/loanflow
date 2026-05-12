package com.api.loanflow.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false, unique = true, length = 14)
	private String cpf;

	@Column(nullable = false, unique = true, length = 160)
	private String email;

	@Column(name = "senha_hash", nullable = false, length = 120)
	private String senhaHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_civil", length = 30)
	private EstadoCivil estadoCivil;

	@Column(length = 60)
	private String nacionalidade;

	@Column(length = 120)
	private String profissao;

	@Column(name = "data_nascimento")
	private LocalDate dataNascimento;

	@Column(length = 20)
	private String telefone;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento_identidade", length = 30)
	private TipoDocumentoIdentidade tipoDocumentoIdentidade;

	@Column(name = "documento_identidade", length = 40)
	private String documentoIdentidade;

	@Column(name = "orgao_emissor", length = 20)
	private String orgaoEmissor;

	@Column(name = "pessoa_exposta_politicamente", nullable = false)
	private boolean pessoaExpostaPoliticamente;

	@Embedded
	private Endereco endereco;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UsuarioStatus status = UsuarioStatus.ATIVO;

	@Enumerated(EnumType.STRING)
	@Column(name = "papel", nullable = false, length = 30)
	private Role papel;

	@Column(name = "data_cadastro", nullable = false)
	private LocalDateTime dataCadastro;

	@Column(name = "data_atualizacao", nullable = false)
	private LocalDateTime dataAtualizacao;

	@PrePersist
	void prePersist() {
		var agora = LocalDateTime.now();
		dataCadastro = agora;
		dataAtualizacao = agora;
	}

	@PreUpdate
	void preUpdate() {
		dataAtualizacao = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public void setSenhaHash(String senhaHash) {
		this.senhaHash = senhaHash;
	}

	public EstadoCivil getEstadoCivil() {
		return estadoCivil;
	}

	public void setEstadoCivil(EstadoCivil estadoCivil) {
		this.estadoCivil = estadoCivil;
	}

	public String getNacionalidade() {
		return nacionalidade;
	}

	public void setNacionalidade(String nacionalidade) {
		this.nacionalidade = nacionalidade;
	}

	public String getProfissao() {
		return profissao;
	}

	public void setProfissao(String profissao) {
		this.profissao = profissao;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public TipoDocumentoIdentidade getTipoDocumentoIdentidade() {
		return tipoDocumentoIdentidade;
	}

	public void setTipoDocumentoIdentidade(TipoDocumentoIdentidade tipoDocumentoIdentidade) {
		this.tipoDocumentoIdentidade = tipoDocumentoIdentidade;
	}

	public String getDocumentoIdentidade() {
		return documentoIdentidade;
	}

	public void setDocumentoIdentidade(String documentoIdentidade) {
		this.documentoIdentidade = documentoIdentidade;
	}

	public String getOrgaoEmissor() {
		return orgaoEmissor;
	}

	public void setOrgaoEmissor(String orgaoEmissor) {
		this.orgaoEmissor = orgaoEmissor;
	}

	public boolean isPessoaExpostaPoliticamente() {
		return pessoaExpostaPoliticamente;
	}

	public void setPessoaExpostaPoliticamente(boolean pessoaExpostaPoliticamente) {
		this.pessoaExpostaPoliticamente = pessoaExpostaPoliticamente;
	}

	public Endereco getEndereco() {
		return endereco;
	}

	public void setEndereco(Endereco endereco) {
		this.endereco = endereco;
	}

	public UsuarioStatus getStatus() {
		return status;
	}

	public void setStatus(UsuarioStatus status) {
		this.status = status;
	}

	public Role getPapel() {
		return papel;
	}

	public void setPapel(Role papel) {
		this.papel = papel;
	}

	public boolean possuiPapel(Role papel) {
		return this.papel == papel;
	}

	public LocalDateTime getDataCadastro() {
		return dataCadastro;
	}

	public LocalDateTime getDataAtualizacao() {
		return dataAtualizacao;
	}
}
