package com.api.loanflow.usuario.application;

import com.api.loanflow.auditoria.application.AuditoriaService;
import com.api.loanflow.shared.exception.RegraNegocioException;
import com.api.loanflow.usuario.domain.Usuario;
import com.api.loanflow.usuario.infrastructure.persistence.AdministradorRepository;
import com.api.loanflow.usuario.infrastructure.persistence.ContaBancariaRepository;
import com.api.loanflow.usuario.infrastructure.persistence.CredorRepository;
import com.api.loanflow.usuario.infrastructure.persistence.SolicitanteCreditoRepository;
import com.api.loanflow.usuario.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceBankAccountRequirementTest {

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
	}

	@Test
	void deveLancarQuandoContaBancariaNaoExiste() {
		when(contaBancariaRepository.existsByUsuarioId(10L)).thenReturn(false);

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> usuarioService.exigirContaBancaria(10L, "Conta bancária obrigatória.")
		);

		assertEquals("Conta bancária obrigatória.", exception.getMessage());
	}

	@Test
	void devePermitirQuandoContaBancariaExiste() {
		when(contaBancariaRepository.existsByUsuarioId(10L)).thenReturn(true);

		assertDoesNotThrow(() -> usuarioService.exigirContaBancaria(10L, "Conta bancária obrigatória."));
	}

	@Test
	void deveUsarSobrecargaComUsuario() {
		var usuario = new Usuario();
		ReflectionTestUtils.setField(usuario, "id", 25L);
		when(contaBancariaRepository.existsByUsuarioId(25L)).thenReturn(false);

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> usuarioService.exigirContaBancaria(usuario, "Usuário precisa de conta.")
		);

		assertEquals("Usuário precisa de conta.", exception.getMessage());
	}
}
