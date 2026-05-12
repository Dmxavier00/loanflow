package com.api.loanflow.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Endereco {
	@Column(length = 9)
	private String cep;

	@Column(length = 160)
	private String logradouro;

	@Column(length = 20)
	private String numero;

	@Column(length = 80)
	private String complemento;

	@Column(length = 80)
	private String bairro;

	@Column(length = 80)
	private String cidade;

	@Column(length = 2)
	private String uf;

	public String getCep() {
		return cep;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public String getLogradouro() {
		return logradouro;
	}

	public void setLogradouro(String logradouro) {
		this.logradouro = logradouro;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public String getComplemento() {
		return complemento;
	}

	public void setComplemento(String complemento) {
		this.complemento = complemento;
	}

	public String getBairro() {
		return bairro;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public String getCidade() {
		return cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

	public String getUf() {
		return uf;
	}

	public void setUf(String uf) {
		this.uf = uf;
	}

	public boolean isInformado() {
		return temTexto(cep)
			|| temTexto(logradouro)
			|| temTexto(numero)
			|| temTexto(complemento)
			|| temTexto(bairro)
			|| temTexto(cidade)
			|| temTexto(uf);
	}

	private boolean temTexto(String valor) {
		return valor != null && !valor.isBlank();
	}
}
