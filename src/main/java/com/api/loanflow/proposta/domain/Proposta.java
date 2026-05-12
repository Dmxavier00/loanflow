package com.api.loanflow.proposta.domain;

import com.api.loanflow.usuario.domain.Credor;
import com.api.loanflow.usuario.domain.SolicitanteCredito;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "propostas")
public class Proposta {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "solicitante_id", nullable = false)
	private SolicitanteCredito solicitante;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "credor_id")
	private Credor credor;

	@Column(name = "valor_solicitado", nullable = false, precision = 18, scale = 2)
	private BigDecimal valorSolicitado;

	@Column(name = "taxa_juros", nullable = false, precision = 8, scale = 4)
	private BigDecimal taxaJuros;

	@Column(name = "prazo_meses", nullable = false)
	private Integer prazoMeses;

	@Column(nullable = false, length = 500)
	private String finalidade;

	@Enumerated(EnumType.STRING)
	@Column(name = "categoria_finalidade", nullable = false, length = 40)
	private CategoriaFinalidade categoriaFinalidade;

	@Column(name = "descricao_detalhada", nullable = false, length = 2000)
	private String descricaoDetalhada;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private PropostaStatus status = PropostaStatus.AGUARDANDO_ACEITE;

	@Column(name = "data_criacao", nullable = false)
	private LocalDateTime dataCriacao;

	@Column(name = "data_atualizacao", nullable = false)
	private LocalDateTime dataAtualizacao;

	@Column(name = "data_expiracao", nullable = false)
	private LocalDate dataExpiracao;

	@PrePersist
	void prePersist() {
		var agora = LocalDateTime.now();
		dataCriacao = agora;
		dataAtualizacao = agora;
	}

	@PreUpdate
	void preUpdate() {
		dataAtualizacao = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public SolicitanteCredito getSolicitante() {
		return solicitante;
	}

	public void setSolicitante(SolicitanteCredito solicitante) {
		this.solicitante = solicitante;
	}

	public Credor getCredor() {
		return credor;
	}

	public void setCredor(Credor credor) {
		this.credor = credor;
	}

	public BigDecimal getValorSolicitado() {
		return valorSolicitado;
	}

	public void setValorSolicitado(BigDecimal valorSolicitado) {
		this.valorSolicitado = valorSolicitado;
	}

	public BigDecimal getTaxaJuros() {
		return taxaJuros;
	}

	public void setTaxaJuros(BigDecimal taxaJuros) {
		this.taxaJuros = taxaJuros;
	}

	public Integer getPrazoMeses() {
		return prazoMeses;
	}

	public void setPrazoMeses(Integer prazoMeses) {
		this.prazoMeses = prazoMeses;
	}

	public String getFinalidade() {
		return finalidade;
	}

	public void setFinalidade(String finalidade) {
		this.finalidade = finalidade;
	}

	public CategoriaFinalidade getCategoriaFinalidade() {
		return categoriaFinalidade;
	}

	public void setCategoriaFinalidade(CategoriaFinalidade categoriaFinalidade) {
		this.categoriaFinalidade = categoriaFinalidade;
	}

	public String getDescricaoDetalhada() {
		return descricaoDetalhada;
	}

	public void setDescricaoDetalhada(String descricaoDetalhada) {
		this.descricaoDetalhada = descricaoDetalhada;
	}

	public PropostaStatus getStatus() {
		return status;
	}

	public void setStatus(PropostaStatus status) {
		this.status = status;
	}

	public LocalDateTime getDataCriacao() {
		return dataCriacao;
	}

	public LocalDateTime getDataAtualizacao() {
		return dataAtualizacao;
	}

	public LocalDate getDataExpiracao() {
		return dataExpiracao;
	}

	public void setDataExpiracao(LocalDate dataExpiracao) {
		this.dataExpiracao = dataExpiracao;
	}
}
