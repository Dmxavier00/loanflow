package com.api.loanflow.compartilhado.cep.aplicacao;

import com.api.loanflow.compartilhado.cep.api.dto.CepLookupResponse;
import com.api.loanflow.compartilhado.excecao.RecursoNaoEncontradoException;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.compartilhado.validacao.DocumentoValidator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class CepLookupService {
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	@Autowired
	public CepLookupService(ObjectMapper objectMapper) {
		this(objectMapper, HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.build());
	}

	CepLookupService(ObjectMapper objectMapper, HttpClient httpClient) {
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
	}

	public CepLookupResponse buscarPorCep(String cep) {
		var cepNormalizado = DocumentoValidator.normalizarCep(cep);
		var request = HttpRequest.newBuilder()
			.uri(URI.create("https://viacep.com.br/ws/" + cepNormalizado + "/json/"))
			.timeout(REQUEST_TIMEOUT)
			.header("Accept", "application/json")
			.GET()
			.build();

		try {
			var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 400) {
				throw new RegraNegocioException("Não foi possível consultar o CEP no momento.");
			}

			var endereco = objectMapper.readValue(response.body(), ViaCepResponse.class);
			if (endereco == null || Boolean.TRUE.equals(endereco.erro())) {
				throw new RecursoNaoEncontradoException("CEP não encontrado.");
			}

			return new CepLookupResponse(
				cepNormalizado,
				normalizarTexto(endereco.logradouro()),
				normalizarTexto(endereco.bairro()),
				normalizarTexto(endereco.localidade()),
				normalizarTexto(endereco.uf())
			);
		} catch (IOException exception) {
			throw new RegraNegocioException("Não foi possível consultar o CEP no momento.");
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new RegraNegocioException("Não foi possível consultar o CEP no momento.");
		}
	}

	private String normalizarTexto(String value) {
		return value == null ? "" : value.trim();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ViaCepResponse(
		String logradouro,
		String bairro,
		String localidade,
		String uf,
		Boolean erro
	) {
	}
}
