package com.api.loanflow.contrato.dominio;

import com.api.loanflow.usuario.dominio.Role;
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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Evidencia eletronica de uma assinatura vinculada a um contrato e a um signatario.
 * Armazena metadados suficientes para trilha de auditoria e validacao do aceite.
 */
@Entity
@Table(
	name = "assinaturas_eletronicas",
	uniqueConstraints = @UniqueConstraint(name = "uk_assinatura_contrato_usuario", columnNames = {"contrato_id", "usuario_id"})
)
public class AssinaturaEletronica {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contrato_id", nullable = false)
	private Contrato contrato;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	/**
	 * Papel desempenhado pelo usuario no fluxo de assinatura do contrato.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "papel_signatario", nullable = false, length = 30)
	private Role papelSignatario;

	/**
	 * Tipo de aceite capturado para compor a prova da assinatura.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_aceite", nullable = false, length = 50)
	private TipoAceite tipoAceite = TipoAceite.ACEITE_WEB_AUTENTICADO;

	/**
	 * Hash calculado sobre os dados da assinatura para detectar adulteracoes.
	 */
	@Column(name = "hash_assinatura", nullable = false, length = 128)
	private String hashAssinatura;

	/**
	 * Endereco IP observado no momento do aceite.
	 */
	@Column(name = "ip_origem", length = 80)
	private String ipOrigem;

	/**
	 * User-Agent enviado pelo cliente para auditoria e rastreabilidade.
	 */
	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@Column(name = "registro_temporal", nullable = false)
	private LocalDateTime registroTemporal;

	/**
	 * Permite invalidar uma evidencia sem apagar o historico persistido.
	 */
	@Column(nullable = false)
	private boolean valida = true;

	@PrePersist
	void prePersist() {
		// Registra o instante exato da criacao da evidencia de assinatura.
		registroTemporal = LocalDateTime.now();
	}

	public Long getId() {
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

	public Role getPapelSignatario() {
		return papelSignatario;
	}

	public void setPapelSignatario(Role papelSignatario) {
		this.papelSignatario = papelSignatario;
	}

	public TipoAceite getTipoAceite() {
		return tipoAceite;
	}

	public void setTipoAceite(TipoAceite tipoAceite) {
		this.tipoAceite = tipoAceite;
	}

	public String getHashAssinatura() {
		return hashAssinatura;
	}

	public void setHashAssinatura(String hashAssinatura) {
		this.hashAssinatura = hashAssinatura;
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

	public LocalDateTime getRegistroTemporal() {
		return registroTemporal;
	}

	public boolean isValida() {
		return valida;
	}

	public void setValida(boolean valida) {
		this.valida = valida;
	}
}
