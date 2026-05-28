package com.api.loanflow.contrato.api;

import com.api.loanflow.contrato.api.dto.ContratoResponse;
import com.api.loanflow.contrato.aplicacao.ContratoService;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import com.api.loanflow.usuario.dominio.UsuarioStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
	void gerarDevePermitirCredorAutenticado() throws Exception {
		var resposta = contratoResponse(7L, ContratoStatus.FORMALIZADO);
		when(contratoService.gerar(10L, "127.0.0.1")).thenReturn(resposta);

		mockMvc.perform(post("/contratos/proposta/10/gerar")
				.with(user("credor@loanflow.test").roles("CREDOR")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(7))
			.andExpect(jsonPath("$.status").value("FORMALIZADO"));

		verify(contratoService).gerar(10L, "127.0.0.1");
	}

	@Test
	void detalharPorIdDevePermitirCredorAutenticado() throws Exception {
		when(contratoService.detalhar(1L)).thenReturn(contratoResponse(1L, ContratoStatus.FORMALIZADO));

		mockMvc.perform(get("/contratos/1")
				.with(user("credor@loanflow.test").roles("CREDOR")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.solicitanteCpf").value("12345678901"))
			.andExpect(jsonPath("$.credorCpf").value("98765432100"))
			.andExpect(jsonPath("$.credorBanco").value("Banco do Brasil"))
			.andExpect(jsonPath("$.credorChavePix").value("credor@pix.test"))
			.andExpect(jsonPath("$.credorStatus").value("ATIVO"))
			.andExpect(jsonPath("$.valorTotalComJuros").value(2687.50))
			.andExpect(jsonPath("$.status").value("FORMALIZADO"));

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
	void listarDevePermitirFiltrarPorCategoria() throws Exception {
		when(contratoService.listarComFiltros(
			ContratoStatus.FORMALIZADO,
			"CTR-2026",
			CategoriaFinalidade.CAPITAL_DE_GIRO,
			"Maria"
		))
			.thenReturn(java.util.List.of());

		mockMvc.perform(get("/contratos")
				.param("status", "FORMALIZADO")
				.param("numeroContrato", "CTR-2026")
				.param("categoriaFinalidade", "CAPITAL_DE_GIRO")
				.param("nomeContraparte", "Maria")
				.with(user("credor@loanflow.test").roles("CREDOR")))
			.andExpect(status().isOk())
			.andExpect(content().json("[]"));

		verify(contratoService).listarComFiltros(
			ContratoStatus.FORMALIZADO,
			"CTR-2026",
			CategoriaFinalidade.CAPITAL_DE_GIRO,
			"Maria"
		);
	}

	@Test
	void cancelarDevePermitirCredorAutenticado() throws Exception {
		when(contratoService.cancelar(7L, "127.0.0.1")).thenReturn(contratoResponse(7L, ContratoStatus.CANCELADO));

		mockMvc.perform(post("/contratos/7/cancelar")
				.with(user("credor@loanflow.test").roles("CREDOR")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELADO"));

		verify(contratoService).cancelar(7L, "127.0.0.1");
	}

	@Test
	void downloadDeveNegarUsuarioSemPapelPermitido() throws Exception {
		mockMvc.perform(get("/contratos/1/download")
				.with(user("visitante@loanflow.test")))
			.andExpect(status().isForbidden());

		verifyNoInteractions(contratoService);
	}

	private ContratoResponse contratoResponse(Long id, ContratoStatus status) {
		return new ContratoResponse(
			id,
			10L,
			"PPT-2026-000010",
			"CTR-2026-000001",
			"Capital de giro",
			101L,
			"Maria Solicitante",
			"12345678901",
			"maria.solicitante@loanflow.test",
			"(11) 99999-0001",
			new BigDecimal("5200.00"),
			85,
			null,
			202L,
			"Credor Teste",
			"98765432100",
			"credor@loanflow.test",
			"(11) 98888-0002",
			"Banco do Brasil",
			"credor@pix.test",
			UsuarioStatus.ATIVO,
			new BigDecimal("50000.00"),
			new BigDecimal("12000.00"),
			10,
			new BigDecimal("2687.50"),
			status,
			"hash-documento",
			"contrato.pdf",
			LocalDateTime.of(2026, 4, 30, 10, 30),
			status == ContratoStatus.FORMALIZADO ? LocalDateTime.of(2026, 4, 30, 10, 31) : null
		);
	}
}
