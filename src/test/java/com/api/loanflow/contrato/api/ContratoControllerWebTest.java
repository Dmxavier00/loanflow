package com.api.loanflow.contrato.api;

import com.api.loanflow.contrato.aplicacao.ContratoService;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.contrato.api.dto.AssinaturaResponse;
import com.api.loanflow.contrato.api.dto.ContratoResponse;
import com.api.loanflow.contrato.api.dto.IniciarDesafioAssinaturaResponse;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.contrato.dominio.MetodoAutenticacaoAssinatura;
import com.api.loanflow.contrato.dominio.TipoAceite;
import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import com.api.loanflow.usuario.dominio.Role;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
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
	void detalharPorIdDevePermitirCredorAutenticado() throws Exception {
		var resposta = new ContratoResponse(
			1L,
			10L,
			"LF-10-ABCDE123",
			"Capital de giro",
			new BigDecimal("2687.50"),
			ContratoStatus.AGUARDANDO_ASSINATURAS,
			"hash-documento",
			"contrato.pdf",
			LocalDateTime.of(2026, 4, 30, 10, 30),
			LocalDateTime.of(2026, 5, 7, 10, 30),
			null,
			null
		);
		when(contratoService.detalhar(1L)).thenReturn(resposta);

		mockMvc.perform(get("/contratos/1")
				.with(user("credor@loanflow.test").roles("CREDOR")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.valorTotalComJuros").value(2687.50))
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
	void listarDevePermitirFiltrarPorCategoria() throws Exception {
		when(contratoService.listarComFiltros(
			ContratoStatus.AGUARDANDO_ASSINATURAS,
			"LF-10",
			CategoriaFinalidade.CAPITAL_DE_GIRO
		))
			.thenReturn(java.util.List.of());

		mockMvc.perform(get("/contratos")
				.param("status", "AGUARDANDO_ASSINATURAS")
				.param("numeroContrato", "LF-10")
				.param("categoriaFinalidade", "CAPITAL_DE_GIRO")
				.with(user("credor@loanflow.test").roles("CREDOR")))
			.andExpect(status().isOk())
			.andExpect(content().json("[]"));

		verify(contratoService).listarComFiltros(
			ContratoStatus.AGUARDANDO_ASSINATURAS,
			"LF-10",
			CategoriaFinalidade.CAPITAL_DE_GIRO
		);
	}

	@Test
	void iniciarDesafioDevePermitirSignatarioAutenticado() throws Exception {
		var desafioId = UUID.randomUUID();
		var resposta = new IniciarDesafioAssinaturaResponse(
			desafioId,
			MetodoAutenticacaoAssinatura.REAUTENTICACAO_SENHA,
			LocalDateTime.of(2026, 5, 13, 14, 40),
			null,
			"Identidade validada. Confirme a assinatura antes do prazo informado."
		);
		when(contratoService.iniciarDesafioAssinatura(
			eq(7L),
			eq(MetodoAutenticacaoAssinatura.REAUTENTICACAO_SENHA),
			eq("127.0.0.1"),
			eq(null)
		))
			.thenReturn(resposta);

		mockMvc.perform(
				post("/contratos/7/assinatura/desafio")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "metodo": "REAUTENTICACAO_SENHA"
						}
						""")
					.with(user("solicitante@loanflow.test").roles("SOLICITANTE"))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.desafioId").value(desafioId.toString()))
			.andExpect(jsonPath("$.metodo").value("REAUTENTICACAO_SENHA"));

		verify(contratoService).iniciarDesafioAssinatura(
			7L,
			MetodoAutenticacaoAssinatura.REAUTENTICACAO_SENHA,
			"127.0.0.1",
			null
		);
	}

	@Test
	void assinarDevePermitirSignatarioAutenticado() throws Exception {
		var resposta = new AssinaturaResponse(
			10L,
			7L,
			5L,
			Role.SOLICITANTE,
			TipoAceite.ACEITE_WEB_AUTENTICADO,
			"hash-assinatura",
			LocalDateTime.of(2026, 5, 13, 14, 42),
			true
		);
		var desafioId = UUID.randomUUID();
		when(contratoService.assinar(eq(7L), eq(desafioId), eq("Senha123!"), eq("127.0.0.1"), eq(null)))
			.thenReturn(resposta);

		mockMvc.perform(
				post("/contratos/7/assinar")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "aceite": true,
						  "desafioId": "%s",
						  "codigo": "Senha123!"
						}
						""".formatted(desafioId))
					.with(user("solicitante@loanflow.test").roles("SOLICITANTE"))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.papelSignatario").value("SOLICITANTE"));

		verify(contratoService).assinar(7L, desafioId, "Senha123!", "127.0.0.1", null);
	}

	@Test
	void assinarDeveRetornarErroQuandoContratoExpirado() throws Exception {
		var desafioId = UUID.randomUUID();
		when(contratoService.assinar(eq(7L), eq(desafioId), eq("Senha123!"), eq("127.0.0.1"), eq(null)))
			.thenThrow(new RegraNegocioException("Prazo de assinatura encerrado."));

		mockMvc.perform(
				post("/contratos/7/assinar")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "aceite": true,
						  "desafioId": "%s",
						  "codigo": "Senha123!"
						}
						""".formatted(desafioId))
					.with(user("solicitante@loanflow.test").roles("SOLICITANTE"))
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Prazo de assinatura encerrado."));
	}

	@Test
	void assinarDeveRetornarErroQuandoDesafioInvalido() throws Exception {
		var desafioId = UUID.randomUUID();
		when(contratoService.assinar(eq(7L), eq(desafioId), eq("000000"), eq("127.0.0.1"), eq(null)))
			.thenThrow(new RegraNegocioException("Desafio de assinatura inválido."));

		mockMvc.perform(
				post("/contratos/7/assinar")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "aceite": true,
						  "desafioId": "%s",
						  "codigo": "000000"
						}
						""".formatted(desafioId))
					.with(user("solicitante@loanflow.test").roles("SOLICITANTE"))
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Desafio de assinatura inválido."));
	}

	@Test
	void assinarDeveRetornarErroQuandoDesafioExpirado() throws Exception {
		var desafioId = UUID.randomUUID();
		when(contratoService.assinar(eq(7L), eq(desafioId), eq("123456"), eq("127.0.0.1"), eq(null)))
			.thenThrow(new RegraNegocioException("Desafio de assinatura expirado. Gere uma nova validação para continuar."));

		mockMvc.perform(
				post("/contratos/7/assinar")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "aceite": true,
						  "desafioId": "%s",
						  "codigo": "123456"
						}
						""".formatted(desafioId))
					.with(user("solicitante@loanflow.test").roles("SOLICITANTE"))
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Desafio de assinatura expirado. Gere uma nova validação para continuar."));
	}

	@Test
	void assinarDeveRetornarErroQuandoUsuarioJaAssinou() throws Exception {
		var desafioId = UUID.randomUUID();
		when(contratoService.assinar(eq(7L), eq(desafioId), eq("Senha123!"), eq("127.0.0.1"), eq(null)))
			.thenThrow(new RegraNegocioException("Usuario ja assinou este contrato."));

		mockMvc.perform(
				post("/contratos/7/assinar")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "aceite": true,
						  "desafioId": "%s",
						  "codigo": "Senha123!"
						}
						""".formatted(desafioId))
					.with(user("solicitante@loanflow.test").roles("SOLICITANTE"))
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Usuario ja assinou este contrato."));
	}

	@Test
	void downloadDeveNegarUsuarioSemPapelPermitido() throws Exception {
		mockMvc.perform(get("/contratos/1/download")
				.with(user("visitante@loanflow.test")))
			.andExpect(status().isForbidden());

		verifyNoInteractions(contratoService);
	}
}
