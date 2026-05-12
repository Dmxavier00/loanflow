package com.api.loanflow.contrato.dominio;

import com.api.loanflow.proposta.dominio.Proposta;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Snapshot persistido do contrato gerado a partir de uma proposta aprovada.
 * Mantem dados de integridade, arquivo emitido e marcos do processo de assinatura.
 */
@Entity
@Table(name = "contratos")
public class Contrato {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proposta_id", nullable = false, unique = true)
	private Proposta proposta;

	@Column(name = "numero_contrato", nullable = false, unique = true, length = 40)
	private String numeroContrato;

	/**
	 * Status atual do processo de assinatura e formalizacao.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ContratoStatus status = ContratoStatus.GERADO;

	/**
	 * Conteudo textual congelado no momento da geracao para evitar divergencia futura.
	 */
	@Lob
	@Column(name = "conteudo_snapshot", nullable = false)
	private String conteudoSnapshot;

	@Column(name = "pdf_path", length = 500)
	private String pdfPath;

	/**
	 * Hash do documento emitido, usado para auditoria e verificacao de integridade.
	 */
	@Column(name = "hash_documento", nullable = false, length = 128)
	private String hashDocumento;

	@Column(name = "data_geracao", nullable = false)
	private LocalDateTime dataGeracao;

	/**
	 * Limite ate quando o contrato pode receber assinaturas.
	 */
	@Column(name = "data_expiracao_assinatura")
	private LocalDateTime dataExpiracaoAssinatura;

	/**
	 * Momento em que o contrato passa a ser considerado formalizado.
	 */
	@Column(name = "data_formalizacao")
	private LocalDateTime dataFormalizacao;

	@PrePersist
	void prePersist() {
		// Garante o registro do instante de geracao mesmo quando nao definido explicitamente.
		dataGeracao = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Proposta getProposta() {
		return proposta;
	}

	public void setProposta(Proposta proposta) {
		this.proposta = proposta;
	}

	public String getNumeroContrato() {
		return numeroContrato;
	}

	public void setNumeroContrato(String numeroContrato) {
		this.numeroContrato = numeroContrato;
	}

	public ContratoStatus getStatus() {
		return status;
	}

	public void setStatus(ContratoStatus status) {
		this.status = status;
	}

	public String getConteudoSnapshot() {
		return conteudoSnapshot;
	}

	public void setConteudoSnapshot(String conteudoSnapshot) {
		this.conteudoSnapshot = conteudoSnapshot;
	}

	public String getPdfPath() {
		return pdfPath;
	}

	public void setPdfPath(String pdfPath) {
		this.pdfPath = pdfPath;
	}

	public String getHashDocumento() {
		return hashDocumento;
	}

	public void setHashDocumento(String hashDocumento) {
		this.hashDocumento = hashDocumento;
	}

	public LocalDateTime getDataGeracao() {
		return dataGeracao;
	}

	public LocalDateTime getDataExpiracaoAssinatura() {
		return dataExpiracaoAssinatura;
	}

	public void setDataExpiracaoAssinatura(LocalDateTime dataExpiracaoAssinatura) {
		this.dataExpiracaoAssinatura = dataExpiracaoAssinatura;
	}

	public LocalDateTime getDataFormalizacao() {
		return dataFormalizacao;
	}

	public void setDataFormalizacao(LocalDateTime dataFormalizacao) {
		this.dataFormalizacao = dataFormalizacao;
	}
}
