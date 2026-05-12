package com.api.loanflow.proposta.api;

import com.api.loanflow.proposta.aplicacao.PropostaService;
import com.api.loanflow.proposta.api.dto.PropostaResponse;
import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import com.api.loanflow.proposta.dominio.PropostaStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropostaControllerWebTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PropostaService propostaService;

	@Test
	void listarDeveResponderJsonSemConflitoComForwardDoSpa() throws Exception {
		var resposta = new PropostaResponse(
			1L,
			2L,
			"Ana Souza",
			null,
			new BigDecimal("4800.00"),
			new BigDecimal("8.9000"),
			6,
			"Tratamento odontologico com implante e exames",
			CategoriaFinalidade.SAUDE,
			"Solicita credito para cobrir implante dentario, radiografias e retorno clinico.",
			PropostaStatus.AGUARDANDO_ACEITE,
			LocalDateTime.of(2026, 4, 29, 10, 0),
			LocalDate.of(2026, 5, 6)
		);

		when(propostaService.listarComFiltros(isNull(), isNull(), isNull(), isNull(), isNull()))
			.thenReturn(List.of(resposta));

		mockMvc.perform(get("/propostas")
				.header("Authorization", "Bearer token-teste")
				.with(user("solicitante@loanflow.test").roles("SOLICITANTE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].solicitanteNome").value("Ana Souza"))
			.andExpect(jsonPath("$[0].categoriaFinalidade").value("SAUDE"))
			.andExpect(jsonPath("$[0].status").value("AGUARDANDO_ACEITE"));

		verify(propostaService).listarComFiltros(null, null, null, null, null);
	}

	@Test
	void criarDeveValidarPrazoMaximoDeDozeParcelas() throws Exception {
		mockMvc.perform(post("/propostas")
				.with(user("solicitante@loanflow.test").roles("SOLICITANTE"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "valorSolicitado": 1500.00,
					  "taxaJuros": 5.00,
					  "prazoMeses": 13,
					  "finalidade": "Capital de giro",
					  "categoriaFinalidade": "CAPITAL_DE_GIRO",
					  "descricaoDetalhada": "Compra de estoque para recomposicao do caixa."
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Dados de entrada inv\u00E1lidos."))
			.andExpect(jsonPath("$.fields.prazoMeses").value("Prazo deve ser de no máximo 12 meses."));

		verifyNoInteractions(propostaService);
	}

	@Test
	void criarDeveValidarTaxaJurosMinima() throws Exception {
		mockMvc.perform(post("/propostas")
				.with(user("solicitante@loanflow.test").roles("SOLICITANTE"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "valorSolicitado": 1500.00,
					  "taxaJuros": 4.99,
					  "prazoMeses": 12,
					  "finalidade": "Capital de giro",
					  "categoriaFinalidade": "CAPITAL_DE_GIRO",
					  "descricaoDetalhada": "Compra de estoque para recomposicao do caixa."
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Dados de entrada inv\u00E1lidos."))
			.andExpect(jsonPath("$.fields.taxaJuros").value("Taxa de juros deve estar entre 5% e 25%."));

		verifyNoInteractions(propostaService);
	}

	@Test
	void criarDeveValidarTaxaJurosMaxima() throws Exception {
		mockMvc.perform(post("/propostas")
				.with(user("solicitante@loanflow.test").roles("SOLICITANTE"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "valorSolicitado": 1500.00,
					  "taxaJuros": 25.01,
					  "prazoMeses": 12,
					  "finalidade": "Capital de giro",
					  "categoriaFinalidade": "CAPITAL_DE_GIRO",
					  "descricaoDetalhada": "Compra de estoque para recomposicao do caixa."
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Dados de entrada inv\u00E1lidos."))
			.andExpect(jsonPath("$.fields.taxaJuros").value("Taxa de juros deve estar entre 5% e 25%."));

		verifyNoInteractions(propostaService);
	}

	@Test
	void detalharPorIdDevePermitirSolicitanteAutenticado() throws Exception {
		var resposta = new PropostaResponse(
			1L,
			2L,
			"Maria Souza",
			4L,
			new BigDecimal("2500.00"),
			new BigDecimal("7.5000"),
			8,
			"Capital de giro",
			CategoriaFinalidade.CAPITAL_DE_GIRO,
			"Reforco de estoque e capital de giro.",
			PropostaStatus.ACEITA,
			LocalDateTime.of(2026, 4, 30, 9, 0),
			LocalDate.of(2026, 5, 7)
		);
		when(propostaService.detalhar(1L)).thenReturn(resposta);

		mockMvc.perform(get("/propostas/1")
				.with(user("solicitante@loanflow.test").roles("SOLICITANTE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.status").value("ACEITA"));

		verify(propostaService).detalhar(1L);
	}

	@Test
	void listarPorStatusDevePermitirCredorAutenticado() throws Exception {
		var resposta = new PropostaResponse(
			1L,
			2L,
			"Maria Souza",
			4L,
			new BigDecimal("2500.00"),
			new BigDecimal("7.5000"),
			8,
			"Capital de giro",
			CategoriaFinalidade.CAPITAL_DE_GIRO,
			"Reforco de estoque e capital de giro.",
			PropostaStatus.APROVADA,
			LocalDateTime.of(2026, 4, 30, 9, 0),
			LocalDate.of(2026, 5, 7)
		);
		when(propostaService.listarPorStatus(PropostaStatus.APROVADA)).thenReturn(List.of(resposta));

		mockMvc.perform(get("/propostas/status/APROVADA")
				.with(user("credor@loanflow.test").roles("CREDOR")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].status").value("APROVADA"));

		verify(propostaService).listarPorStatus(PropostaStatus.APROVADA);
	}

	@Test
	void listarPorStatusDeveNegarUsuarioSemPapelPermitido() throws Exception {
		mockMvc.perform(get("/propostas/status/APROVADA")
				.with(user("visitante@loanflow.test")))
			.andExpect(status().isForbidden());

		verifyNoInteractions(propostaService);
	}
}
