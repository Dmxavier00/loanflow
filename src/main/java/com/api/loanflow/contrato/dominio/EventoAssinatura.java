package com.api.loanflow.contrato.dominio;

import com.api.loanflow.usuario.dominio.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_assinatura")
public class EventoAssinatura {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contrato_id", nullable = false)
	private Contrato contrato;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private EventoAssinaturaTipo tipo;

	@Column(length = 500)
	private String detalhes;

	@Column(name = "ip_origem", length = 80)
	private String ipOrigem;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@Column(name = "data_hora", nullable = false)
	private LocalDateTime dataHora;

	@PrePersist
	void prePersist() {
		if (dataHora == null) {
			dataHora = LocalDateTime.now();
		}
	}

	public void setContrato(Contrato contrato) {
		this.contrato = contrato;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public void setTipo(EventoAssinaturaTipo tipo) {
		this.tipo = tipo;
	}

	public void setDetalhes(String detalhes) {
		this.detalhes = detalhes;
	}

	public void setIpOrigem(String ipOrigem) {
		this.ipOrigem = ipOrigem;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
}
