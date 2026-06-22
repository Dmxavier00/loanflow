package com.api.loanflow.usuario.dominio;

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
import java.math.RoundingMode;

@Entity
@Table(name = "solicitantes_credito")
public class SolicitanteCredito {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, unique = true)
	private Usuario usuario;

	@Column(name = "renda_mensal", precision = 18, scale = 2)
	private BigDecimal rendaMensal;

	@Column(name = "tipo_ocupacao", length = 80)
	private String tipoOcupacao;

	@Column(name = "score_credito_simulado")
	private Integer scoreCreditoSimulado;

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public BigDecimal getRendaMensal() {
		return rendaMensal;
	}

	public void setRendaMensal(BigDecimal rendaMensal) {
		this.rendaMensal = rendaMensal;
	}

	public String getTipoOcupacao() {
		return tipoOcupacao;
	}

	public void setTipoOcupacao(String tipoOcupacao) {
		this.tipoOcupacao = tipoOcupacao;
	}

	public Integer getScoreCreditoSimulado() {
		return normalizarScore(scoreCreditoSimulado);
	}

	public void setScoreCreditoSimulado(Integer scoreCreditoSimulado) {
		this.scoreCreditoSimulado = normalizarScore(scoreCreditoSimulado);
	}

	public NivelRiscoCredito getNivelRisco() {
		return NivelRiscoCredito.fromScore(getScoreCreditoSimulado());
	}

	public void recalcularScoreCreditoSimulado() {
		setScoreCreditoSimulado(calcularScoreCreditoSimulado(rendaMensal));
	}

	public static int calcularScoreCreditoSimulado(BigDecimal rendaMensal) {
		if (rendaMensal == null || rendaMensal.signum() <= 0) {
			return 0;
		}
		if (rendaMensal.compareTo(new BigDecimal("5000.00")) >= 0) {
			return 85;
		}
		if (rendaMensal.compareTo(new BigDecimal("3000.00")) >= 0) {
			return 75;
		}
		if (rendaMensal.compareTo(new BigDecimal("1500.00")) >= 0) {
			return 55;
		}
		return 35;
	}

	private Integer normalizarScore(Integer score) {
		if (score == null) {
			return null;
		}
		if (score > 100) {
			return BigDecimal.valueOf(score)
				.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP)
				.min(BigDecimal.valueOf(100))
				.intValue();
		}
		return Math.max(0, score);
	}
}
