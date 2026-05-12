package com.api.loanflow.usuario.application;

import com.api.loanflow.auditoria.application.AuditoriaService;
import com.api.loanflow.usuario.api.dto.AlterarSenhaRequest;
import com.api.loanflow.usuario.api.dto.AtualizarDadosFinanceirosRequest;
import com.api.loanflow.usuario.domain.Credor;
import com.api.loanflow.usuario.domain.Role;
import com.api.loanflow.usuario.domain.SolicitanteCredito;
import com.api.loanflow.usuario.domain.Usuario;
import com.api.loanflow.usuario.infrastructure.persistence.AdministradorRepository;
import com.api.loanflow.usuario.infrastructure.persistence.ContaBancariaRepository;
import com.api.loanflow.usuario.infrastructure.persistence.CredorRepository;
import com.api.loanflow.usuario.infrastructure.persistence.SolicitanteCreditoRepository;
import com.api.loanflow.usuario.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceAccountSettingsTest {

	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private SolicitanteCreditoRepository solicitanteRepository;
	@Mock
	private CredorRepository credorRepository;
	@Mock
	private AdministradorRepository administradorRepository;
	@Mock
	private ContaBancariaRepository contaBancariaRepository;
	@Mock
	private AuditoriaService auditoriaService;
	@Mock
	private PasswordEncoder passwordEncoder;

	private UsuarioService usuarioService;

	@BeforeEach
	void setUp() {
		usuarioService = new UsuarioService(
			usuarioRepository,
			solicitanteRepository,
			credorRepository,
			administradorRepository,
			contaBancariaRepository,
			auditoriaService,
			passwordEncoder
		);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("usuario@loanflow.test", "senha")
		);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void alterarMinhaSenhaDeveAtualizarHashQuandoSenhaForDiferenteDaAtual() {
		var usuario = usuario(10L, Role.SOLICITANTE, "hash-atual");
		when(usuarioRepository.findByEmail("usuario@loanflow.test")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("NovaSenha456", "hash-atual")).thenReturn(false);
		when(passwordEncoder.encode("NovaSenha456")).thenReturn("hash-nova");

		usuarioService.alterarMinhaSenha(new AlterarSenhaRequest("NovaSenha456"));

		assertEquals("hash-nova", usuario.getSenhaHash());
		verify(passwordEncoder).encode("NovaSenha456");
	}

	@Test
	void alterarMinhaSenhaDeveManterHashQuandoSenhaInformadaJaForAAtual() {
		var usuario = usuario(10L, Role.SOLICITANTE, "hash-atual");
		when(usuarioRepository.findByEmail("usuario@loanflow.test")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("SenhaAtual123", "hash-atual")).thenReturn(true);

		usuarioService.alterarMinhaSenha(new AlterarSenhaRequest("SenhaAtual123"));

		assertEquals("hash-atual", usuario.getSenhaHash());
		verify(passwordEncoder, never()).encode(anyString());
	}

	@Test
	void atualizarMeusDadosFinanceirosDeveAtualizarRendaDoSolicitante() {
		var usuario = usuario(10L, Role.SOLICITANTE, "hash-atual");
		var solicitante = solicitante(usuario, 21L, new BigDecimal("3200.00"));
		when(usuarioRepository.findByEmail("usuario@loanflow.test")).thenReturn(Optional.of(usuario));
		when(solicitanteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(solicitante));
		when(credorRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());
		when(administradorRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());
		when(contaBancariaRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());

		var response = usuarioService.atualizarMeusDadosFinanceiros(
			new AtualizarDadosFinanceirosRequest(new BigDecimal("4800.00"), null, null)
		);

		assertEquals(new BigDecimal("4800.00"), solicitante.getRendaMensal());
		assertEquals(new BigDecimal("4800.00"), response.rendaMensal());
		assertEquals(21L, response.solicitanteId());
	}

	@Test
	void atualizarMeusDadosFinanceirosDeveAtualizarSaldoELimiteDoCredor() {
		var usuario = usuario(11L, Role.CREDOR, "hash-atual");
		var credor = credor(usuario, 31L, new BigDecimal("15000.00"), 4);
		when(usuarioRepository.findByEmail("usuario@loanflow.test")).thenReturn(Optional.of(usuario));
		when(solicitanteRepository.findByUsuarioId(11L)).thenReturn(Optional.empty());
		when(credorRepository.findByUsuarioId(11L)).thenReturn(Optional.of(credor));
		when(administradorRepository.findByUsuarioId(11L)).thenReturn(Optional.empty());
		when(contaBancariaRepository.findByUsuarioId(11L)).thenReturn(Optional.empty());

		var response = usuarioService.atualizarMeusDadosFinanceiros(
			new AtualizarDadosFinanceirosRequest(null, new BigDecimal("25000.00"), 7)
		);

		assertEquals(new BigDecimal("25000.00"), credor.getSaldoDisponivelSimulado());
		assertEquals(7, credor.getLimiteOperacoes());
		assertEquals(new BigDecimal("25000.00"), response.saldoDisponivelSimulado());
		assertEquals(7, response.limiteOperacoes());
	}

	private Usuario usuario(Long id, Role papel, String senhaHash) {
		var usuario = new Usuario();
		ReflectionTestUtils.setField(usuario, "id", id);
		usuario.setEmail("usuario@loanflow.test");
		usuario.setSenhaHash(senhaHash);
		usuario.setPapel(papel);
		return usuario;
	}

	private SolicitanteCredito solicitante(Usuario usuario, Long id, BigDecimal rendaMensal) {
		var solicitante = new SolicitanteCredito();
		ReflectionTestUtils.setField(solicitante, "id", id);
		solicitante.setUsuario(usuario);
		solicitante.setRendaMensal(rendaMensal);
		return solicitante;
	}

	private Credor credor(Usuario usuario, Long id, BigDecimal saldoDisponivelSimulado, Integer limiteOperacoes) {
		var credor = new Credor();
		ReflectionTestUtils.setField(credor, "id", id);
		credor.setUsuario(usuario);
		credor.setSaldoDisponivelSimulado(saldoDisponivelSimulado);
		credor.setLimiteOperacoes(limiteOperacoes);
		return credor;
	}
}
