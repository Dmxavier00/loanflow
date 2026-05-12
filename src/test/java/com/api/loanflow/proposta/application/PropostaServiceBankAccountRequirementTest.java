package com.api.loanflow.proposta.application;

import com.api.loanflow.auditoria.application.AuditoriaService;
import com.api.loanflow.notificacao.application.NotificacaoService;
import com.api.loanflow.proposta.api.dto.AtualizarPropostaRequest;
import com.api.loanflow.proposta.api.dto.CriarPropostaRequest;
import com.api.loanflow.proposta.domain.CategoriaFinalidade;
import com.api.loanflow.proposta.domain.Proposta;
import com.api.loanflow.proposta.domain.PropostaStatus;
import com.api.loanflow.proposta.infrastructure.persistence.PropostaRepository;
import com.api.loanflow.shared.exception.RegraNegocioException;
import com.api.loanflow.usuario.application.UsuarioService;
import com.api.loanflow.usuario.domain.Credor;
import com.api.loanflow.usuario.domain.Role;
import com.api.loanflow.usuario.domain.SolicitanteCredito;
import com.api.loanflow.usuario.domain.Usuario;
import com.api.loanflow.usuario.infrastructure.persistence.CredorRepository;
import com.api.loanflow.usuario.infrastructure.persistence.SolicitanteCreditoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropostaServiceBankAccountRequirementTest {

	@Mock
	private PropostaRepository propostaRepository;
	@Mock
	private SolicitanteCreditoRepository solicitanteRepository;
	@Mock
	private CredorRepository credorRepository;
	@Mock
	private UsuarioService usuarioService;
	@Mock
	private AuditoriaService auditoriaService;
	@Mock
	private NotificacaoService notificacaoService;
	@Mock
	private PoliticaCreditoService politicaCreditoService;
	@Mock
	private PoliticaCredorService politicaCredorService;

	private PropostaService propostaService;

	@BeforeEach
	void setUp() {
		propostaService = new PropostaService(
			propostaRepository,
			solicitanteRepository,
			credorRepository,
			usuarioService,
			auditoriaService,
			notificacaoService,
			politicaCreditoService,
			politicaCredorService
		);
	}

	@Test
	void criarDeveExigirContaBancariaDoSolicitante() {
		var usuario = usuario(1L, Role.SOLICITANTE);
		var solicitante = solicitante(101L, usuario);
		var request = criarRequest();
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(solicitanteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(solicitante));
		doThrow(new RegraNegocioException("Cadastre uma conta bancária antes de criar uma proposta."))
			.when(usuarioService).exigirContaBancaria(eq(usuario), contains("criar uma proposta"));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> propostaService.criar(request, "127.0.0.1")
		);

		assertEquals("Cadastre uma conta bancária antes de criar uma proposta.", exception.getMessage());
		verify(propostaRepository, never()).save(org.mockito.ArgumentMatchers.any(Proposta.class));
	}

	@Test
	void criarDeveAplicarPoliticaDeCreditoDoSolicitante() {
		var usuario = usuario(11L, Role.SOLICITANTE);
		var solicitante = solicitante(111L, usuario);
		var request = criarRequest();
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(solicitanteRepository.findByUsuarioId(11L)).thenReturn(Optional.of(solicitante));
		when(propostaRepository.save(any(Proposta.class))).thenAnswer(invocation -> invocation.getArgument(0));

		propostaService.criar(request, "127.0.0.1");

		verify(politicaCreditoService).validarNovaContratacao(
			solicitante,
			request.valorSolicitado(),
			request.taxaJuros(),
			request.prazoMeses()
		);
	}

	@Test
	void criarDeveDefinirExpiracaoAutomaticaDeSeteDias() {
		var usuario = usuario(12L, Role.SOLICITANTE);
		var solicitante = solicitante(112L, usuario);
		var request = criarRequest();
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(solicitanteRepository.findByUsuarioId(12L)).thenReturn(Optional.of(solicitante));
		when(propostaRepository.save(any(Proposta.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = propostaService.criar(request, "127.0.0.1");

		assertEquals(LocalDate.now().plusDays(7), response.dataExpiracao());
	}

	@Test
	void atualizarDeveExigirContaBancariaDoSolicitante() {
		var usuario = usuario(2L, Role.SOLICITANTE);
		var solicitante = solicitante(202L, usuario);
		var proposta = proposta(302L, solicitante, null, PropostaStatus.RASCUNHO);
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(propostaRepository.findById(302L)).thenReturn(Optional.of(proposta));
		doThrow(new RegraNegocioException("Cadastre uma conta bancária antes de atualizar a proposta."))
			.when(usuarioService).exigirContaBancaria(eq(usuario), contains("atualizar a proposta"));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> propostaService.atualizar(302L, atualizarRequest(), "127.0.0.1")
		);

		assertEquals("Cadastre uma conta bancária antes de atualizar a proposta.", exception.getMessage());
	}

	@Test
	void submeterDeveExigirContaBancariaDoSolicitante() {
		var usuario = usuario(3L, Role.SOLICITANTE);
		var solicitante = solicitante(203L, usuario);
		var proposta = proposta(303L, solicitante, null, PropostaStatus.RASCUNHO);
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(propostaRepository.findById(303L)).thenReturn(Optional.of(proposta));
		doThrow(new RegraNegocioException("Cadastre uma conta bancária antes de submeter a proposta."))
			.when(usuarioService).exigirContaBancaria(eq(usuario), contains("submeter a proposta"));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> propostaService.submeter(303L, "127.0.0.1")
		);

		assertEquals("Cadastre uma conta bancária antes de submeter a proposta.", exception.getMessage());
	}

	@Test
	void iniciarAnaliseDeveExigirContaBancariaDoCredor() {
		var usuario = usuario(4L, Role.CREDOR);
		var solicitante = solicitante(204L, usuario(40L, Role.SOLICITANTE));
		var credor = credor(304L, usuario);
		var proposta = proposta(404L, solicitante, credor, PropostaStatus.ACEITA);
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(propostaRepository.findById(404L)).thenReturn(Optional.of(proposta));
		when(credorRepository.findByUsuarioId(4L)).thenReturn(Optional.of(credor));
		doThrow(new RegraNegocioException("Cadastre uma conta bancária antes de iniciar a análise de propostas."))
			.when(usuarioService).exigirContaBancaria(eq(usuario), anyString());

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> propostaService.iniciarAnalise(404L, "127.0.0.1")
		);

		assertEquals("Cadastre uma conta bancária antes de iniciar a análise de propostas.", exception.getMessage());
	}

	@Test
	void iniciarAnaliseDeveExigirPropostaAceita() {
		var usuario = usuario(14L, Role.CREDOR);
		var solicitante = solicitante(214L, usuario(140L, Role.SOLICITANTE));
		var credor = credor(314L, usuario);
		var proposta = proposta(414L, solicitante, credor, PropostaStatus.AGUARDANDO_ACEITE);
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(propostaRepository.findById(414L)).thenReturn(Optional.of(proposta));
		when(credorRepository.findByUsuarioId(14L)).thenReturn(Optional.of(credor));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> propostaService.iniciarAnalise(414L, "127.0.0.1")
		);

		assertEquals("Somente proposta aceita pode entrar em análise.", exception.getMessage());
	}

	@Test
	void aceitarDeveExigirContaBancariaDoCredor() {
		var usuario = usuario(5L, Role.CREDOR);
		var solicitante = solicitante(205L, usuario(50L, Role.SOLICITANTE));
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		doThrow(new RegraNegocioException("Cadastre uma conta bancária antes de aceitar uma proposta."))
			.when(usuarioService).exigirContaBancaria(eq(usuario), contains("aceitar uma proposta"));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> propostaService.aceitar(405L, "127.0.0.1")
		);

		assertEquals("Cadastre uma conta bancária antes de aceitar uma proposta.", exception.getMessage());
	}

	@Test
	void aceitarDeveAplicarPoliticaDeCreditoDoSolicitante() {
		var usuario = usuario(15L, Role.CREDOR);
		var solicitante = solicitante(215L, usuario(150L, Role.SOLICITANTE));
		var credor = credor(315L, usuario);
		var proposta = proposta(415L, solicitante, null, PropostaStatus.AGUARDANDO_ACEITE);
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(credorRepository.findByUsuarioId(15L)).thenReturn(Optional.of(credor));
		when(propostaRepository.findById(415L)).thenReturn(Optional.of(proposta));

		propostaService.aceitar(415L, "127.0.0.1");

		verify(politicaCreditoService).validarNovaContratacao(
			solicitante,
			proposta.getValorSolicitado(),
			proposta.getTaxaJuros(),
			proposta.getPrazoMeses()
		);
		verify(politicaCredorService).validarDisponibilidade(credor, proposta);
	}

	@Test
	void aprovarDeveExigirContaBancariaDoCredor() {
		var usuario = usuario(6L, Role.CREDOR);
		var solicitante = solicitante(206L, usuario(60L, Role.SOLICITANTE));
		var credor = credor(306L, usuario);
		var proposta = proposta(406L, solicitante, credor, PropostaStatus.EM_ANALISE);
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(propostaRepository.findById(406L)).thenReturn(Optional.of(proposta));
		doThrow(new RegraNegocioException("Cadastre uma conta bancária antes de aprovar a proposta."))
			.when(usuarioService).exigirContaBancaria(eq(usuario), contains("aprovar a proposta"));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> propostaService.aprovar(406L, "127.0.0.1")
		);

		assertEquals("Cadastre uma conta bancária antes de aprovar a proposta.", exception.getMessage());
	}

	@Test
	void aprovarDeveAplicarPoliticaDeCreditoDoSolicitante() {
		var usuario = usuario(16L, Role.CREDOR);
		var solicitante = solicitante(216L, usuario(160L, Role.SOLICITANTE));
		var credor = credor(316L, usuario);
		var proposta = proposta(416L, solicitante, credor, PropostaStatus.EM_ANALISE);
		when(usuarioService.usuarioAtual()).thenReturn(usuario);
		when(propostaRepository.findById(416L)).thenReturn(Optional.of(proposta));

		propostaService.aprovar(416L, "127.0.0.1");

		verify(politicaCreditoService).validarNovaContratacao(
			solicitante,
			proposta.getValorSolicitado(),
			proposta.getTaxaJuros(),
			proposta.getPrazoMeses()
		);
		verify(politicaCredorService).validarDisponibilidade(credor, proposta);
	}

	private CriarPropostaRequest criarRequest() {
		return new CriarPropostaRequest(
			new BigDecimal("1500.00"),
			new BigDecimal("2.50"),
			12,
			"Capital de giro",
			CategoriaFinalidade.CAPITAL_DE_GIRO,
			"Compra de equipamentos e reforco do caixa."
		);
	}

	private AtualizarPropostaRequest atualizarRequest() {
		return new AtualizarPropostaRequest(
			new BigDecimal("2000.00"),
			new BigDecimal("2.00"),
			10,
			"Ajuste do pedido",
			CategoriaFinalidade.OUTRA,
			"Ajuste de valor e prazo antes do aceite."
		);
	}

	private Usuario usuario(Long id, Role... roles) {
		var usuario = new Usuario();
		ReflectionTestUtils.setField(usuario, "id", id);
		usuario.setPapel(roles[0]);
		return usuario;
	}

	private SolicitanteCredito solicitante(Long id, Usuario usuario) {
		var solicitante = new SolicitanteCredito();
		ReflectionTestUtils.setField(solicitante, "id", id);
		solicitante.setUsuario(usuario);
		return solicitante;
	}

	private Credor credor(Long id, Usuario usuario) {
		var credor = new Credor();
		ReflectionTestUtils.setField(credor, "id", id);
		credor.setUsuario(usuario);
		return credor;
	}

	private Proposta proposta(Long id, SolicitanteCredito solicitante, Credor credor, PropostaStatus status) {
		var proposta = new Proposta();
		ReflectionTestUtils.setField(proposta, "id", id);
		proposta.setSolicitante(solicitante);
		proposta.setCredor(credor);
		proposta.setStatus(status);
		proposta.setFinalidade("Teste");
		proposta.setCategoriaFinalidade(CategoriaFinalidade.OUTRA);
		proposta.setDescricaoDetalhada("Detalhamento de teste.");
		proposta.setDataExpiracao(java.time.LocalDate.now().plusDays(10));
		proposta.setValorSolicitado(new BigDecimal("1000.00"));
		proposta.setTaxaJuros(new BigDecimal("1.50"));
		proposta.setPrazoMeses(6);
		return proposta;
	}
}
