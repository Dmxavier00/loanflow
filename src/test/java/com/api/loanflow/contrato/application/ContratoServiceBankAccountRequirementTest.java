package com.api.loanflow.contrato.application;

import com.api.loanflow.auditoria.application.AuditoriaService;
import com.api.loanflow.contrato.domain.Contrato;
import com.api.loanflow.contrato.domain.ContratoStatus;
import com.api.loanflow.contrato.infrastructure.persistence.AssinaturaEletronicaRepository;
import com.api.loanflow.contrato.infrastructure.persistence.ContratoRepository;
import com.api.loanflow.notificacao.application.NotificacaoService;
import com.api.loanflow.parcela.application.ParcelaService;
import com.api.loanflow.proposta.application.PoliticaCreditoService;
import com.api.loanflow.proposta.application.PropostaService;
import com.api.loanflow.proposta.domain.CategoriaFinalidade;
import com.api.loanflow.proposta.domain.Proposta;
import com.api.loanflow.proposta.domain.PropostaStatus;
import com.api.loanflow.shared.crypto.HashService;
import com.api.loanflow.shared.exception.RegraNegocioException;
import com.api.loanflow.usuario.application.UsuarioService;
import com.api.loanflow.usuario.domain.Credor;
import com.api.loanflow.usuario.domain.Role;
import com.api.loanflow.usuario.domain.SolicitanteCredito;
import com.api.loanflow.usuario.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoServiceBankAccountRequirementTest {

	@Mock
	private ContratoRepository contratoRepository;
	@Mock
	private AssinaturaEletronicaRepository assinaturaRepository;
	@Mock
	private PropostaService propostaService;
	@Mock
	private UsuarioService usuarioService;
	@Mock
	private HashService hashService;
	@Mock
	private PdfContratoService pdfContratoService;
	@Mock
	private ParcelaService parcelaService;
	@Mock
	private AuditoriaService auditoriaService;
	@Mock
	private NotificacaoService notificacaoService;
	@Mock
	private PoliticaCreditoService politicaCreditoService;

	private ContratoService contratoService;

	@BeforeEach
	void setUp() {
		contratoService = new ContratoService(
			contratoRepository,
			assinaturaRepository,
			propostaService,
			usuarioService,
			hashService,
			pdfContratoService,
			parcelaService,
			auditoriaService,
			notificacaoService,
			politicaCreditoService
		);
	}

	@Test
	void gerarDeveExigirContaBancariaDoSolicitanteECredor() {
		var credorUsuario = usuario(10L, Role.CREDOR);
		var solicitanteUsuario = usuario(20L, Role.SOLICITANTE);
		var proposta = proposta(100L, solicitanteUsuario, credorUsuario, PropostaStatus.APROVADA);
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaService.buscarComPermissao(100L)).thenReturn(proposta);
		when(contratoRepository.findByPropostaId(100L)).thenReturn(Optional.empty());
		doThrow(new RegraNegocioException("Solicitante precisa cadastrar conta bancária antes de prosseguir com o contrato."))
			.when(usuarioService).exigirContaBancaria(eq(solicitanteUsuario), contains("Solicitante precisa"));

		assertThrows(RegraNegocioException.class, () -> contratoService.gerar(100L, "127.0.0.1"));

		verify(usuarioService, never()).exigirContaBancaria(eq(credorUsuario), contains("Credor precisa"));
		verify(contratoRepository, never()).save(any(Contrato.class));
	}

	@Test
	void gerarDeveVerificarSolicitanteAntesDoCredor() {
		var credorUsuario = usuario(11L, Role.CREDOR);
		var solicitanteUsuario = usuario(21L, Role.SOLICITANTE);
		var proposta = proposta(101L, solicitanteUsuario, credorUsuario, PropostaStatus.APROVADA);
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaService.buscarComPermissao(101L)).thenReturn(proposta);
		when(contratoRepository.findByPropostaId(101L)).thenReturn(Optional.empty());
		when(hashService.sha256(any(String.class))).thenReturn("hash");
		when(pdfContratoService.gerarContratoPdf(any(String.class), any(String.class))).thenReturn("contrato.pdf");
		when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> invocation.getArgument(0));

		contratoService.gerar(101L, "127.0.0.1");

		InOrder inOrder = inOrder(usuarioService);
		inOrder.verify(usuarioService).usuarioAtual();
		inOrder.verify(usuarioService).exigirContaBancaria(eq(solicitanteUsuario), contains("Solicitante precisa"));
		inOrder.verify(usuarioService).exigirContaBancaria(eq(credorUsuario), contains("Credor precisa"));
	}

	@Test
	void assinarDeveExigirContaBancariaDosParticipantes() {
		var credorUsuario = usuario(12L, Role.CREDOR);
		var solicitanteUsuario = usuario(22L, Role.SOLICITANTE);
		var proposta = proposta(102L, solicitanteUsuario, credorUsuario, PropostaStatus.APROVADA);
		var contrato = contrato(202L, proposta, ContratoStatus.AGUARDANDO_ASSINATURAS);
		when(usuarioService.usuarioAtual()).thenReturn(solicitanteUsuario);
		when(contratoRepository.findById(202L)).thenReturn(Optional.of(contrato));
		doThrow(new RegraNegocioException("Solicitante precisa cadastrar conta bancária antes de prosseguir com o contrato."))
			.when(usuarioService).exigirContaBancaria(eq(solicitanteUsuario), contains("Solicitante precisa"));

		assertThrows(RegraNegocioException.class, () -> contratoService.assinar(202L, "127.0.0.1", "JUnit"));

		verify(usuarioService, never()).exigirContaBancaria(eq(credorUsuario), contains("Credor precisa"));
		verify(assinaturaRepository, never()).save(any());
	}

	@Test
	void gerarDeveRevalidarPoliticaDeCreditoDoSolicitante() {
		var credorUsuario = usuario(13L, Role.CREDOR);
		var solicitanteUsuario = usuario(23L, Role.SOLICITANTE);
		var proposta = proposta(103L, solicitanteUsuario, credorUsuario, PropostaStatus.APROVADA);
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaService.buscarComPermissao(103L)).thenReturn(proposta);
		when(contratoRepository.findByPropostaId(103L)).thenReturn(Optional.empty());
		when(hashService.sha256(any(String.class))).thenReturn("hash");
		when(pdfContratoService.gerarContratoPdf(any(String.class), any(String.class))).thenReturn("contrato.pdf");
		when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> {
			var contrato = invocation.getArgument(0, Contrato.class);
			ReflectionTestUtils.setField(contrato, "id", 903L);
			return contrato;
		});

		contratoService.gerar(103L, "127.0.0.1");

		verify(politicaCreditoService).validarNovaContratacao(
			proposta.getSolicitante(),
			proposta.getValorSolicitado(),
			proposta.getTaxaJuros(),
			proposta.getPrazoMeses()
		);
	}

	@Test
	void gerarDeveMontarConteudoEstruturadoParaPdf() {
		var credorUsuario = usuario(14L, Role.CREDOR);
		var solicitanteUsuario = usuario(24L, Role.SOLICITANTE);
		solicitanteUsuario.setCpf("12345678901");
		credorUsuario.setCpf("98765432100");
		var proposta = proposta(104L, solicitanteUsuario, credorUsuario, PropostaStatus.APROVADA);
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaService.buscarComPermissao(104L)).thenReturn(proposta);
		when(contratoRepository.findByPropostaId(104L)).thenReturn(Optional.empty());
		when(hashService.sha256(any(String.class))).thenReturn("hash");
		var conteudoCaptor = ArgumentCaptor.forClass(String.class);
		when(pdfContratoService.gerarContratoPdf(any(String.class), conteudoCaptor.capture())).thenReturn("contrato.pdf");
		when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> invocation.getArgument(0));

		contratoService.gerar(104L, "127.0.0.1");

		var conteudo = conteudoCaptor.getValue();
		assertTrue(conteudo.contains("# 1. Resumo executivo"));
		assertTrue(conteudo.contains("# 2. Identificação das partes"));
		assertTrue(conteudo.contains("# 7. Integridade, auditoria e limitações do protótipo"));
		assertTrue(conteudo.contains("CPF: 123.456.789-01"));
		assertTrue(conteudo.contains("Solicitante: ______________________________________________"));
	}

	@Test
	void gerarDeveExigirPropostaAprovada() {
		var credorUsuario = usuario(15L, Role.CREDOR);
		var solicitanteUsuario = usuario(25L, Role.SOLICITANTE);
		var proposta = proposta(105L, solicitanteUsuario, credorUsuario, PropostaStatus.ACEITA);
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaService.buscarComPermissao(105L)).thenReturn(proposta);

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> contratoService.gerar(105L, "127.0.0.1")
		);

		assertTrue(exception.getMessage().contains("proposta aprovada"));
		verify(contratoRepository, never()).findByPropostaId(105L);
	}

	private Usuario usuario(Long id, Role... roles) {
		var usuario = new Usuario();
		ReflectionTestUtils.setField(usuario, "id", id);
		usuario.setPapel(roles[0]);
		usuario.setNome("Usuario " + id);
		return usuario;
	}

	private Proposta proposta(Long id, Usuario solicitanteUsuario, Usuario credorUsuario, PropostaStatus status) {
		var solicitante = new SolicitanteCredito();
		ReflectionTestUtils.setField(solicitante, "id", id + 1000);
		solicitante.setUsuario(solicitanteUsuario);

		var credor = new Credor();
		ReflectionTestUtils.setField(credor, "id", id + 2000);
		credor.setUsuario(credorUsuario);

		var proposta = new Proposta();
		ReflectionTestUtils.setField(proposta, "id", id);
		proposta.setSolicitante(solicitante);
		proposta.setCredor(credor);
		proposta.setStatus(status);
		proposta.setValorSolicitado(new BigDecimal("1000.00"));
		proposta.setTaxaJuros(new BigDecimal("1.99"));
		proposta.setPrazoMeses(6);
		proposta.setFinalidade("Teste");
		proposta.setCategoriaFinalidade(CategoriaFinalidade.OUTRA);
		proposta.setDescricaoDetalhada("Detalhamento usado nos testes de contrato.");
		proposta.setDataExpiracao(LocalDate.now().plusDays(7));
		return proposta;
	}

	private Contrato contrato(Long id, Proposta proposta, ContratoStatus status) {
		var contrato = new Contrato();
		ReflectionTestUtils.setField(contrato, "id", id);
		contrato.setProposta(proposta);
		contrato.setStatus(status);
		contrato.setNumeroContrato("LF-" + id);
		contrato.setHashDocumento("hash-documento");
		contrato.setConteudoSnapshot("conteudo");
		contrato.setPdfPath("contrato.pdf");
		contrato.setDataExpiracaoAssinatura(LocalDateTime.now().plusDays(3));
		return contrato;
	}
}
