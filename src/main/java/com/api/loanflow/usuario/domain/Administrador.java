package com.api.loanflow.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "administradores")
public class Administrador {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, unique = true)
	private Usuario usuario;

	@Column(name = "nivel_acesso", length = 50)
	private String nivelAcesso;

	@Column(name = "setor_responsavel", length = 80)
	private String setorResponsavel;

	@Column(name = "data_designacao")
	private LocalDate dataDesignacao;

	@Column(name = "status_administrativo", length = 40)
	private String statusAdministrativo;

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public String getNivelAcesso() {
		return nivelAcesso;
	}

	public void setNivelAcesso(String nivelAcesso) {
		this.nivelAcesso = nivelAcesso;
	}

	public String getSetorResponsavel() {
		return setorResponsavel;
	}

	public void setSetorResponsavel(String setorResponsavel) {
		this.setorResponsavel = setorResponsavel;
	}

	public LocalDate getDataDesignacao() {
		return dataDesignacao;
	}

	public void setDataDesignacao(LocalDate dataDesignacao) {
		this.dataDesignacao = dataDesignacao;
	}

	public String getStatusAdministrativo() {
		return statusAdministrativo;
	}

	public void setStatusAdministrativo(String statusAdministrativo) {
		this.statusAdministrativo = statusAdministrativo;
	}
}
