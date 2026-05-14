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
import java.util.UUID;

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

	@Enumerated(EnumType.STRING)
	@Column(name = "papel_signatario", nullable = false, length = 30)
	private Role papelSignatario;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_aceite", nullable = false, length = 50)
	private TipoAceite tipoAceite = TipoAceite.ACEITE_WEB_AUTENTICADO;

	@Column(name = "hash_assinatura", nullable = false, length = 128)
	private String hashAssinatura;

	@Column(name = "hash_conteudo_aceito", length = 128)
	private String hashConteudoAceito;

	@Column(name = "versao_termo_aceite", length = 40)
	private String versaoTermoAceite;

	@Enumerated(EnumType.STRING)
	@Column(name = "metodo_autenticacao", length = 40)
	private MetodoAutenticacaoAssinatura metodoAutenticacao;

	@Column(name = "desafio_autenticacao_id")
	private UUID desafioAutenticacaoId;

	@Column(name = "assinatura_documento_valida")
	private Boolean assinaturaDocumentoValida;

	@Column(name = "ip_origem", length = 80)
	private String ipOrigem;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@Column(name = "registro_temporal", nullable = false)
	private LocalDateTime registroTemporal;

	@Column(nullable = false)
	private boolean valida = true;

	@PrePersist
	void prePersist() {
		if (registroTemporal == null) {
			registroTemporal = LocalDateTime.now();
		}
	}

	public void preencherMetadadosAceite(
		Contrato contrato,
		DesafioAssinatura desafio,
		String versaoTermoAceite,
		boolean documentoIntegro
	) {
		hashConteudoAceito = contrato.getHashDocumento();
		this.versaoTermoAceite = versaoTermoAceite;
		metodoAutenticacao = desafio.getMetodoAutenticacao();
		desafioAutenticacaoId = desafio.getId();
		assinaturaDocumentoValida = documentoIntegro;
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

	public String getHashConteudoAceito() {
		return hashConteudoAceito;
	}

	public void setHashConteudoAceito(String hashConteudoAceito) {
		this.hashConteudoAceito = hashConteudoAceito;
	}

	public String getVersaoTermoAceite() {
		return versaoTermoAceite;
	}

	public void setVersaoTermoAceite(String versaoTermoAceite) {
		this.versaoTermoAceite = versaoTermoAceite;
	}

	public MetodoAutenticacaoAssinatura getMetodoAutenticacao() {
		return metodoAutenticacao;
	}

	public void setMetodoAutenticacao(MetodoAutenticacaoAssinatura metodoAutenticacao) {
		this.metodoAutenticacao = metodoAutenticacao;
	}

	public UUID getDesafioAutenticacaoId() {
		return desafioAutenticacaoId;
	}

	public void setDesafioAutenticacaoId(UUID desafioAutenticacaoId) {
		this.desafioAutenticacaoId = desafioAutenticacaoId;
	}

	public Boolean getAssinaturaDocumentoValida() {
		return assinaturaDocumentoValida;
	}

	public void setAssinaturaDocumentoValida(Boolean assinaturaDocumentoValida) {
		this.assinaturaDocumentoValida = assinaturaDocumentoValida;
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
