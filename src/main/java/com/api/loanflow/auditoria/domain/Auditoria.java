package com.api.loanflow.auditoria.domain;

import com.api.loanflow.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditorias")
public class Auditoria {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private AuditoriaAcao acao;

	@Column(name = "entidade_tipo", nullable = false, length = 80)
	private String entidadeTipo;

	@Column(name = "entidade_id")
	private Long entidadeId;

	@Column(name = "data_hora", nullable = false)
	private LocalDateTime dataHora;

	@Column(name = "ip_origem", length = 80)
	private String ipOrigem;

	@Lob
	private String detalhes;

	@PrePersist
	void prePersist() {
		dataHora = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public AuditoriaAcao getAcao() {
		return acao;
	}

	public void setAcao(AuditoriaAcao acao) {
		this.acao = acao;
	}

	public String getEntidadeTipo() {
		return entidadeTipo;
	}

	public void setEntidadeTipo(String entidadeTipo) {
		this.entidadeTipo = entidadeTipo;
	}

	public Long getEntidadeId() {
		return entidadeId;
	}

	public void setEntidadeId(Long entidadeId) {
		this.entidadeId = entidadeId;
	}

	public LocalDateTime getDataHora() {
		return dataHora;
	}

	public String getIpOrigem() {
		return ipOrigem;
	}

	public void setIpOrigem(String ipOrigem) {
		this.ipOrigem = ipOrigem;
	}

	public String getDetalhes() {
		return detalhes;
	}

	public void setDetalhes(String detalhes) {
		this.detalhes = detalhes;
	}
}
