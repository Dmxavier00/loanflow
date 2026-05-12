package com.api.loanflow.parcela.domain;

import com.api.loanflow.contrato.domain.Contrato;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
	name = "parcelas",
	uniqueConstraints = @UniqueConstraint(name = "uk_parcela_contrato_numero", columnNames = {"contrato_id", "numero"})
)
public class Parcela {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contrato_id", nullable = false)
	private Contrato contrato;

	@Column(nullable = false)
	private Integer numero;

	@Column(name = "valor_previsto", nullable = false, precision = 18, scale = 2)
	private BigDecimal valorPrevisto;

	@Column(name = "valor_pago_acumulado", nullable = false, precision = 18, scale = 2)
	private BigDecimal valorPagoAcumulado = BigDecimal.ZERO;

	@Column(name = "data_vencimento", nullable = false)
	private LocalDate dataVencimento;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ParcelaStatus status = ParcelaStatus.ABERTA;

	public Long getId() {
		return id;
	}

	public Contrato getContrato() {
		return contrato;
	}

	public void setContrato(Contrato contrato) {
		this.contrato = contrato;
	}

	public Integer getNumero() {
		return numero;
	}

	public void setNumero(Integer numero) {
		this.numero = numero;
	}

	public BigDecimal getValorPrevisto() {
		return valorPrevisto;
	}

	public void setValorPrevisto(BigDecimal valorPrevisto) {
		this.valorPrevisto = valorPrevisto;
	}

	public BigDecimal getValorPagoAcumulado() {
		return valorPagoAcumulado;
	}

	public void setValorPagoAcumulado(BigDecimal valorPagoAcumulado) {
		this.valorPagoAcumulado = valorPagoAcumulado;
	}

	public LocalDate getDataVencimento() {
		return dataVencimento;
	}

	public void setDataVencimento(LocalDate dataVencimento) {
		this.dataVencimento = dataVencimento;
	}

	public ParcelaStatus getStatus() {
		return status;
	}

	public void setStatus(ParcelaStatus status) {
		this.status = status;
	}
}
