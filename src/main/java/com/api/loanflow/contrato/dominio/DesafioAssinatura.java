package com.api.loanflow.contrato.dominio;

import com.api.loanflow.usuario.dominio.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "desafios_assinatura")
public class DesafioAssinatura {
	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contrato_id", nullable = false)
	private Contrato contrato;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "codigo_hash", length = 128)
	private String codigoHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "metodo_autenticacao", nullable = false, length = 40)
	private MetodoAutenticacaoAssinatura metodoAutenticacao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private DesafioAssinaturaStatus status;

	@Column(name = "ip_origem", length = 80)
	private String ipOrigem;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@Column(name = "criado_em", nullable = false)
	private LocalDateTime criadoEm;

	@Column(name = "expira_em", nullable = false)
	private LocalDateTime expiraEm;

	@Column(name = "validado_em")
	private LocalDateTime validadoEm;

	@Column(name = "consumido_em")
	private LocalDateTime consumidoEm;

	@Column(nullable = false)
	private int tentativas;

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (status == null) {
			status = DesafioAssinaturaStatus.PENDENTE;
		}
		if (criadoEm == null) {
			criadoEm = LocalDateTime.now();
		}
	}

	public boolean expiradoEm(LocalDateTime instante) {
		return expiraEm != null && !expiraEm.isAfter(instante);
	}

	public boolean estaAberto() {
		return status == DesafioAssinaturaStatus.PENDENTE || status == DesafioAssinaturaStatus.VALIDADO;
	}

	public void marcarValidado(LocalDateTime instante) {
		status = DesafioAssinaturaStatus.VALIDADO;
		validadoEm = instante;
	}

	public void consumir(LocalDateTime instante) {
		status = DesafioAssinaturaStatus.CONSUMIDO;
		consumidoEm = instante;
	}

	public void expirar(LocalDateTime instante) {
		status = DesafioAssinaturaStatus.EXPIRADO;
		consumidoEm = instante;
	}

	public void cancelar(LocalDateTime instante) {
		status = DesafioAssinaturaStatus.CANCELADO;
		consumidoEm = instante;
	}

	public UUID getId() {
		return id;
	}

	public Contrato getContrato() {
		return contrato;
	}

	public void setContrato(Contrato contrato) {
		this.contrato = contrato;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public String getCodigoHash() {
		return codigoHash;
	}

	public void setCodigoHash(String codigoHash) {
		this.codigoHash = codigoHash;
	}

	public MetodoAutenticacaoAssinatura getMetodoAutenticacao() {
		return metodoAutenticacao;
	}

	public void setMetodoAutenticacao(MetodoAutenticacaoAssinatura metodoAutenticacao) {
		this.metodoAutenticacao = metodoAutenticacao;
	}

	public DesafioAssinaturaStatus getStatus() {
		return status;
	}

	public void setStatus(DesafioAssinaturaStatus status) {
		this.status = status;
	}

	public String getIpOrigem() {
		return ipOrigem;
	}

	public void setIpOrigem(String ipOrigem) {
		this.ipOrigem = ipOrigem;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public LocalDateTime getExpiraEm() {
		return expiraEm;
	}

	public void setExpiraEm(LocalDateTime expiraEm) {
		this.expiraEm = expiraEm;
	}

	public LocalDateTime getValidadoEm() {
		return validadoEm;
	}

	public LocalDateTime getConsumidoEm() {
		return consumidoEm;
	}

	public int getTentativas() {
		return tentativas;
	}

	public void incrementarTentativas() {
		tentativas++;
	}
}
