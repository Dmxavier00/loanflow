package com.api.loanflow.contrato.api;

import com.api.loanflow.contrato.application.ContratoService;
import com.api.loanflow.contrato.api.dto.ContratoResponse;
import com.api.loanflow.contrato.domain.ContratoStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoControllerWebTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ContratoService contratoService;

	@Test
	void detalharPorIdDevePermitirCredorAutenticado() throws Exception {
		var resposta = new ContratoResponse(
			1L,
			10L,
			"LF-10-ABCDE123",
			"Capital de giro",
			ContratoStatus.AGUARDANDO_ASSINATURAS,
			"hash-documento",
			"contrato.pdf",
			LocalDateTime.of(2026, 4, 30, 10, 30),
			null
		);
		when(contratoService.detalhar(1L)).thenReturn(resposta);

		mockMvc.perform(get("/contratos/1")
				.with(user("credor@loanflow.test").roles("CREDOR")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.status").value("AGUARDANDO_ASSINATURAS"));

		verify(contratoService).detalhar(1L);
	}

	@Test
	void downloadDevePermitirSolicitanteAutenticado() throws Exception {
		when(contratoService.baixarPdf(1L)).thenReturn("pdf-teste".getBytes());

		mockMvc.perform(get("/contratos/1/download")
				.with(user("solicitante@loanflow.test").roles("SOLICITANTE")))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", "attachment; filename=contrato-1.pdf"))
			.andExpect(content().contentType(MediaType.APPLICATION_PDF))
			.andExpect(content().bytes("pdf-teste".getBytes()));

		verify(contratoService).baixarPdf(1L);
	}

	@Test
	void downloadDeveNegarUsuarioSemPapelPermitido() throws Exception {
		mockMvc.perform(get("/contratos/1/download")
				.with(user("visitante@loanflow.test")))
			.andExpect(status().isForbidden());

		verifyNoInteractions(contratoService);
	}
}
