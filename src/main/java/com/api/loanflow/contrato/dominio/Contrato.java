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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ContratoStatus status = ContratoStatus.GERADO;

	@Lob
	@Column(name = "conteudo_snapshot", nullable = false)
	private String conteudoSnapshot;

	@Column(name = "pdf_path", length = 500)
	private String pdfPath;

	@Column(name = "hash_documento", nullable = false, length = 128)
	private String hashDocumento;

	@Column(name = "hash_pdf_emitido", length = 128)
	private String hashPdfEmitido;

	@Column(name = "data_geracao", nullable = false)
	private LocalDateTime dataGeracao;

	@Column(name = "data_expiracao_assinatura")
	private LocalDateTime dataExpiracaoAssinatura;

	@Column(name = "data_formalizacao")
	private LocalDateTime dataFormalizacao;

	@Column(name = "data_ultima_verificacao_assinatura")
	private LocalDateTime dataUltimaVerificacaoAssinatura;

	@Column(name = "motivo_expiracao", length = 240)
	private String motivoExpiracao;

	@PrePersist
	void prePersist() {
		if (dataGeracao == null) {
			dataGeracao = LocalDateTime.now();
		}
	}

	public boolean podeReceberAssinaturas() {
		return status == ContratoStatus.AGUARDANDO_ASSINATURAS || status == ContratoStatus.ASSINADO_PARCIALMENTE;
	}

	public boolean estaExpirado(LocalDateTime instante) {
		return podeReceberAssinaturas()
			&& dataExpiracaoAssinatura != null
			&& dataExpiracaoAssinatura.isBefore(instante);
	}

	public void marcarExpirado(String motivo, LocalDateTime instante) {
		if (!podeReceberAssinaturas()) {
			return;
		}
		status = ContratoStatus.EXPIRADO;
		motivoExpiracao = motivo;
		dataUltimaVerificacaoAssinatura = instante;
	}

	public void registrarVerificacaoAssinatura(LocalDateTime instante) {
		dataUltimaVerificacaoAssinatura = instante;
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

	public String getHashPdfEmitido() {
		return hashPdfEmitido;
	}

	public void setHashPdfEmitido(String hashPdfEmitido) {
		this.hashPdfEmitido = hashPdfEmitido;
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

	public LocalDateTime getDataUltimaVerificacaoAssinatura() {
		return dataUltimaVerificacaoAssinatura;
	}

	public String getMotivoExpiracao() {
		return motivoExpiracao;
	}

	public void setMotivoExpiracao(String motivoExpiracao) {
		this.motivoExpiracao = motivoExpiracao;
	}
}
