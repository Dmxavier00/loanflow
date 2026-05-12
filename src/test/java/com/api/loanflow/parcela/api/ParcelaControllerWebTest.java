package com.api.loanflow.parcela.api;

import com.api.loanflow.parcela.aplicacao.ParcelaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParcelaControllerWebTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ParcelaService parcelaService;

	@Test
	void detalharPorIdDeveSerRestritoAoAdmin() throws Exception {
		mockMvc.perform(get("/parcelas/1")
				.with(user("solicitante@loanflow.test").roles("SOLICITANTE")))
			.andExpect(status().isForbidden());

		verifyNoInteractions(parcelaService);
	}

	@Test
	void listarPorContratoIdDeveSerRestritoAoAdmin() throws Exception {
		mockMvc.perform(get("/contratos/1/parcelas")
				.with(user("credor@loanflow.test").roles("CREDOR")))
			.andExpect(status().isForbidden());

		verifyNoInteractions(parcelaService);
	}
}
