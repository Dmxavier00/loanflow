package com.api.loanflow.shared.cep.api;

import com.api.loanflow.shared.cep.api.dto.CepLookupResponse;
import com.api.loanflow.shared.cep.application.CepLookupService;
import com.api.loanflow.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CepLookupControllerWebTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CepLookupService cepLookupService;

	@Test
	void buscarPorCepDeveSerPublicoERetornarEnderecoEncontrado() throws Exception {
		when(cepLookupService.buscarPorCep("01001000"))
			.thenReturn(new CepLookupResponse("01001-000", "Praca da Se", "Centro", "Sao Paulo", "SP"));

		mockMvc.perform(get("/enderecos/cep/01001000"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cep").value("01001-000"))
			.andExpect(jsonPath("$.logradouro").value("Praca da Se"))
			.andExpect(jsonPath("$.bairro").value("Centro"))
			.andExpect(jsonPath("$.cidade").value("Sao Paulo"))
			.andExpect(jsonPath("$.uf").value("SP"));
	}

	@Test
	void buscarPorCepDeveRetornarNotFoundQuandoServicoNaoEncontrarCep() throws Exception {
		when(cepLookupService.buscarPorCep("99999999"))
			.thenThrow(new RecursoNaoEncontradoException("CEP não encontrado."));

		mockMvc.perform(get("/enderecos/cep/99999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.message").value("CEP não encontrado."));
	}
}
