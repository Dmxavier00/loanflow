package com.api.loanflow.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "credores")
public class Credor {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, unique = true)
	private Usuario usuario;

	@Column(name = "saldo_disponivel_simulado", nullable = false, precision = 18, scale = 2)
	private BigDecimal saldoDisponivelSimulado = BigDecimal.ZERO;

	@Column(name = "total_emprestado_simulado", nullable = false, precision = 18, scale = 2)
	private BigDecimal totalEmprestadoSimulado = BigDecimal.ZERO;

	@Column(name = "limite_operacoes", nullable = false)
	private Integer limiteOperacoes = 10;

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public BigDecimal getSaldoDisponivelSimulado() {
		return saldoDisponivelSimulado;
	}

	public void setSaldoDisponivelSimulado(BigDecimal saldoDisponivelSimulado) {
		this.saldoDisponivelSimulado = saldoDisponivelSimulado;
	}

	public BigDecimal getTotalEmprestadoSimulado() {
		return totalEmprestadoSimulado;
	}

	public void setTotalEmprestadoSimulado(BigDecimal totalEmprestadoSimulado) {
		this.totalEmprestadoSimulado = totalEmprestadoSimulado;
	}

	public Integer getLimiteOperacoes() {
		return limiteOperacoes;
	}

	public void setLimiteOperacoes(Integer limiteOperacoes) {
		this.limiteOperacoes = limiteOperacoes;
	}
}
