package com.api.loanflow.usuario.api;

import com.api.loanflow.compartilhado.excecao.RecursoNaoEncontradoException;
import com.api.loanflow.usuario.api.dto.ContaBancariaResponse;
import com.api.loanflow.usuario.api.dto.UsuarioResponse;
import com.api.loanflow.usuario.aplicacao.UsuarioService;
import com.api.loanflow.usuario.dominio.Role;
import com.api.loanflow.usuario.dominio.TipoContaBancaria;
import com.api.loanflow.usuario.dominio.UsuarioStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioControllerWebTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UsuarioService usuarioService;

	@Test
	void meDeveRetornarContaBancariaIdNoPerfil() throws Exception {
		when(usuarioService.meuPerfil()).thenReturn(usuarioResponse(77L));

		mockMvc.perform(get("/usuarios/me").with(user("usuario@loanflow.test").roles("SOLICITANTE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10))
			.andExpect(jsonPath("$.email").value("usuario@loanflow.test"))
			.andExpect(jsonPath("$.contaBancariaId").value(77));
	}

	@Test
	void minhaContaBancariaDeveRetornarDadosQuandoExiste() throws Exception {
		when(usuarioService.minhaContaBancaria()).thenReturn(contaBancariaResponse());

		mockMvc.perform(get("/usuarios/me/conta-bancaria").with(user("usuario@loanflow.test").roles("SOLICITANTE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(99))
			.andExpect(jsonPath("$.usuarioId").value(10))
			.andExpect(jsonPath("$.banco").value("Banco do Brasil"))
			.andExpect(jsonPath("$.tipoConta").value("CORRENTE"));
	}

	@Test
	void minhaContaBancariaDeveRetornarNotFoundQuandoNaoExiste() throws Exception {
		when(usuarioService.minhaContaBancaria())
			.thenThrow(new RecursoNaoEncontradoException("Conta bancária não cadastrada."));

		mockMvc.perform(get("/usuarios/me/conta-bancaria").with(user("usuario@loanflow.test").roles("SOLICITANTE")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.message").value("Conta bancária não cadastrada."));
	}

	@Test
	void salvarMinhaContaBancariaDeveRetornarContaAtualizada() throws Exception {
		when(usuarioService.salvarMinhaContaBancaria(any())).thenReturn(contaBancariaResponse());

		mockMvc.perform(put("/usuarios/me/conta-bancaria")
				.with(user("usuario@loanflow.test").roles("SOLICITANTE"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "banco": "Banco do Brasil",
					  "agencia": "1234-5",
					  "numeroConta": "987654-3",
					  "tipoConta": "CORRENTE",
					  "chavePix": "usuario@email.com"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(99))
			.andExpect(jsonPath("$.numeroConta").value("987654-3"))
			.andExpect(jsonPath("$.chavePix").value("usuario@email.com"));
	}

	@Test
	void salvarMinhaContaBancariaDeveValidarCamposObrigatorios() throws Exception {
		mockMvc.perform(put("/usuarios/me/conta-bancaria")
				.with(user("usuario@loanflow.test").roles("SOLICITANTE"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "banco": " ",
					  "agencia": "1234-5",
					  "numeroConta": "987654-3",
					  "chavePix": "usuario@email.com"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Dados de entrada inválidos."))
			.andExpect(jsonPath("$.fields.banco").exists())
			.andExpect(jsonPath("$.fields.tipoConta").exists());
	}

	@Test
	void alterarMinhaSenhaDeveRetornarNoContent() throws Exception {
		mockMvc.perform(put("/usuarios/me/senha")
				.with(user("usuario@loanflow.test").roles("SOLICITANTE"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "senha": "NovaSenha456"
					}
					"""))
			.andExpect(status().isNoContent());
	}

	@Test
	void atualizarMeusDadosFinanceirosDeveRetornarPerfilAtualizado() throws Exception {
		when(usuarioService.atualizarMeusDadosFinanceiros(any())).thenReturn(usuarioResponse(77L));

		mockMvc.perform(put("/usuarios/me/dados-financeiros")
				.with(user("usuario@loanflow.test").roles("SOLICITANTE"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "rendaMensal": 4800.00
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rendaMensal").value(4800.00))
			.andExpect(jsonPath("$.contaBancariaId").value(77));
	}

	@Test
	void minhaContaBancariaDeveExigirAutenticacao() throws Exception {
		mockMvc.perform(get("/usuarios/me/conta-bancaria"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.error").value("Unauthorized"))
			.andExpect(jsonPath("$.message").value("Autenticação obrigatória."));

		verifyNoInteractions(usuarioService);
	}

	private UsuarioResponse usuarioResponse(Long contaBancariaId) {
		return new UsuarioResponse(
			10L,
			"Usuario Teste",
			"123.456.789-00",
			"usuario@loanflow.test",
			null,
			"Brasileira",
			"Analista",
			null,
			"(11) 99999-9999",
			null,
			null,
			null,
			false,
			null,
			UsuarioStatus.ATIVO,
			Role.SOLICITANTE,
			LocalDateTime.of(2026, 4, 27, 10, 0),
			20L,
			null,
			null,
			contaBancariaId,
			new java.math.BigDecimal("4800.00"),
			null,
			null
		);
	}

	private ContaBancariaResponse contaBancariaResponse() {
		return new ContaBancariaResponse(
			99L,
			10L,
			"Banco do Brasil",
			"1234-5",
			"987654-3",
			TipoContaBancaria.CORRENTE,
			"usuario@email.com",
			LocalDateTime.of(2026, 4, 27, 10, 0),
			LocalDateTime.of(2026, 4, 27, 11, 0)
		);
	}
}
