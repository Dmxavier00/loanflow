package com.api.loanflow.pagamento.domain;

import com.api.loanflow.parcela.domain.Parcela;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos")
public class Pagamento {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parcela_id", nullable = false)
	private Parcela parcela;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_registrador_id", nullable = false)
	private Usuario usuarioRegistrador;

	@Column(name = "valor_pago", nullable = false, precision = 18, scale = 2)
	private BigDecimal valorPago;

	@Column(name = "data_hora_pagamento", nullable = false)
	private LocalDateTime dataHoraPagamento;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento", nullable = false, length = 40)
	private FormaPagamento formaPagamento;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private PagamentoStatus status = PagamentoStatus.REGISTRADO;

	@Column(length = 500)
	private String comprovante;

	@PrePersist
	void prePersist() {
		if (dataHoraPagamento == null) {
			dataHoraPagamento = LocalDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public Parcela getParcela() {
		return parcela;
	}

	public void setParcela(Parcela parcela) {
		this.parcela = parcela;
	}

	public Usuario getUsuarioRegistrador() {
		return usuarioRegistrador;
	}

	public void setUsuarioRegistrador(Usuario usuarioRegistrador) {
		this.usuarioRegistrador = usuarioRegistrador;
	}

	public BigDecimal getValorPago() {
		return valorPago;
	}

	public void setValorPago(BigDecimal valorPago) {
		this.valorPago = valorPago;
	}

	public LocalDateTime getDataHoraPagamento() {
		return dataHoraPagamento;
	}

	public void setDataHoraPagamento(LocalDateTime dataHoraPagamento) {
		this.dataHoraPagamento = dataHoraPagamento;
	}

	public FormaPagamento getFormaPagamento() {
		return formaPagamento;
	}

	public void setFormaPagamento(FormaPagamento formaPagamento) {
		this.formaPagamento = formaPagamento;
	}

	public PagamentoStatus getStatus() {
		return status;
	}

	public void setStatus(PagamentoStatus status) {
		this.status = status;
	}

	public String getComprovante() {
		return comprovante;
	}

	public void setComprovante(String comprovante) {
		this.comprovante = comprovante;
	}
}
