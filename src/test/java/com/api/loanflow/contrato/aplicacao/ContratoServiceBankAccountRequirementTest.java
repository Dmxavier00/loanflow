package com.api.loanflow.contrato.aplicacao;

import com.api.loanflow.auditoria.aplicacao.AuditoriaService;
import com.api.loanflow.compartilhado.aplicacao.NumeroNegocioService;
import com.api.loanflow.compartilhado.criptografia.HashService;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.contrato.infraestrutura.persistencia.ContratoRepository;
import com.api.loanflow.notificacao.aplicacao.NotificacaoService;
import com.api.loanflow.parcela.aplicacao.ParcelaService;
import com.api.loanflow.proposta.aplicacao.PoliticaCreditoService;
import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import com.api.loanflow.proposta.dominio.Proposta;
import com.api.loanflow.proposta.dominio.PropostaStatus;
import com.api.loanflow.proposta.infraestrutura.persistencia.PropostaRepository;
import com.api.loanflow.usuario.aplicacao.UsuarioService;
import com.api.loanflow.usuario.dominio.ContaBancaria;
import com.api.loanflow.usuario.dominio.Credor;
import com.api.loanflow.usuario.dominio.Role;
import com.api.loanflow.usuario.dominio.SolicitanteCredito;
import com.api.loanflow.usuario.dominio.Usuario;
import com.api.loanflow.usuario.infraestrutura.persistencia.ContaBancariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
	private PropostaRepository propostaRepository;
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
	@Mock
	private ContaBancariaRepository contaBancariaRepository;

	private ContratoService contratoService;
	private NumeroNegocioService numeroNegocioService;
	private ContratoConteudoService contratoConteudoService;

	@BeforeEach
	void setUp() {
		numeroNegocioService = new NumeroNegocioService();
		contratoConteudoService = new ContratoConteudoService(contaBancariaRepository);
		contratoService = new ContratoService(
			contratoRepository,
			propostaRepository,
			usuarioService,
			hashService,
			pdfContratoService,
			parcelaService,
			auditoriaService,
			notificacaoService,
			politicaCreditoService,
			contratoConteudoService,
			numeroNegocioService,
			contaBancariaRepository
		);
	}

	@Test
	void gerarDeveExigirContaBancariaDoSolicitanteECredor() {
		var credorUsuario = usuario(10L, Role.CREDOR);
		var solicitanteUsuario = usuario(20L, Role.SOLICITANTE);
		var proposta = proposta(100L, solicitanteUsuario, credorUsuario, PropostaStatus.APROVADA);
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaRepository.findById(100L)).thenReturn(Optional.of(proposta));
		when(contratoRepository.findByPropostaId(100L)).thenReturn(Optional.empty());
		doThrow(new RegraNegocioException("Solicitante precisa cadastrar conta bancária antes de prosseguir com o contrato."))
			.when(usuarioService).exigirContaBancaria(eq(solicitanteUsuario), contains("Solicitante precisa"));

		assertThrows(RegraNegocioException.class, () -> contratoService.gerar(100L, "127.0.0.1"));

		verify(usuarioService, never()).exigirContaBancaria(eq(credorUsuario), contains("Credor precisa"));
		verify(contratoRepository, never()).save(any(Contrato.class));
		verify(contratoRepository, never()).saveAndFlush(any(Contrato.class));
	}

	@Test
	void gerarDeveVerificarSolicitanteAntesDoCredor() {
		var credorUsuario = usuario(11L, Role.CREDOR);
		var solicitanteUsuario = usuario(21L, Role.SOLICITANTE);
		var proposta = proposta(101L, solicitanteUsuario, credorUsuario, PropostaStatus.APROVADA);
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaRepository.findById(101L)).thenReturn(Optional.of(proposta));
		when(contratoRepository.findByPropostaId(101L)).thenReturn(Optional.empty());
		when(hashService.sha256(any(String.class))).thenReturn("hash");
		when(hashService.sha256(any(byte[].class))).thenReturn("hash-pdf");
		when(pdfContratoService.gerarContratoPdf(any(String.class), any(String.class))).thenReturn("contrato.pdf");
		when(pdfContratoService.lerPdf("contrato.pdf")).thenReturn("pdf".getBytes());
		when(contratoRepository.saveAndFlush(any(Contrato.class))).thenAnswer(invocation -> {
			var contrato = invocation.getArgument(0, Contrato.class);
			ReflectionTestUtils.setField(contrato, "id", 901L);
			ReflectionTestUtils.setField(contrato, "dataGeracao", LocalDate.now().atStartOfDay());
			return contrato;
		});
		when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> invocation.getArgument(0));

		contratoService.gerar(101L, "127.0.0.1");

		InOrder inOrder = inOrder(usuarioService);
		inOrder.verify(usuarioService).usuarioAtual();
		inOrder.verify(usuarioService).exigirContaBancaria(eq(solicitanteUsuario), contains("Solicitante precisa"));
		inOrder.verify(usuarioService).exigirContaBancaria(eq(credorUsuario), contains("Credor precisa"));
	}

	@Test
	void gerarDeveRevalidarPoliticaDeCreditoDoSolicitante() {
		var credorUsuario = usuario(13L, Role.CREDOR);
		var solicitanteUsuario = usuario(23L, Role.SOLICITANTE);
		var proposta = proposta(103L, solicitanteUsuario, credorUsuario, PropostaStatus.APROVADA);
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaRepository.findById(103L)).thenReturn(Optional.of(proposta));
		when(contratoRepository.findByPropostaId(103L)).thenReturn(Optional.empty());
		when(hashService.sha256(any(String.class))).thenReturn("hash");
		when(hashService.sha256(any(byte[].class))).thenReturn("hash-pdf");
		when(pdfContratoService.gerarContratoPdf(any(String.class), any(String.class))).thenReturn("contrato.pdf");
		when(pdfContratoService.lerPdf("contrato.pdf")).thenReturn("pdf".getBytes());
		when(contratoRepository.saveAndFlush(any(Contrato.class))).thenAnswer(invocation -> {
			var contrato = invocation.getArgument(0, Contrato.class);
			ReflectionTestUtils.setField(contrato, "id", 903L);
			ReflectionTestUtils.setField(contrato, "dataGeracao", LocalDate.now().atStartOfDay());
			return contrato;
		});
		when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
		var credorContaBancaria = new ContaBancaria();
		credorContaBancaria.setBanco("Banco do Brasil");
		credorContaBancaria.setChavePix("credor@pix.test");
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaRepository.findById(104L)).thenReturn(Optional.of(proposta));
		when(contratoRepository.findByPropostaId(104L)).thenReturn(Optional.empty());
		when(contaBancariaRepository.findByUsuarioId(14L)).thenReturn(Optional.of(credorContaBancaria));
		when(hashService.sha256(any(String.class))).thenReturn("hash");
		when(hashService.sha256(any(byte[].class))).thenReturn("hash-pdf");
		var conteudoCaptor = ArgumentCaptor.forClass(String.class);
		when(pdfContratoService.gerarContratoPdf(any(String.class), conteudoCaptor.capture())).thenReturn("contrato.pdf");
		when(pdfContratoService.lerPdf("contrato.pdf")).thenReturn("pdf".getBytes());
		when(contratoRepository.saveAndFlush(any(Contrato.class))).thenAnswer(invocation -> {
			var contrato = invocation.getArgument(0, Contrato.class);
			ReflectionTestUtils.setField(contrato, "id", 904L);
			ReflectionTestUtils.setField(contrato, "dataGeracao", LocalDate.now().atStartOfDay());
			return contrato;
		});
		when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> invocation.getArgument(0));

		contratoService.gerar(104L, "127.0.0.1");

		var conteudo = conteudoCaptor.getValue();
		assertTrue(conteudo.contains("# 1. Resumo da operação"));
		assertTrue(conteudo.contains("# 2. Partes"));
		assertTrue(conteudo.contains("# 4. Integridade e observação"));
		assertTrue(conteudo.contains("CPF: 123.456.789-01"));
		assertTrue(conteudo.contains("Banco: Banco do Brasil"));
		assertTrue(conteudo.contains("Chave Pix: credor@pix.test"));
		assertTrue(conteudo.contains("Solicitante: ______________________________________________"));
		assertTrue(conteudo.contains("# 3. Condições registradas"));
		assertTrue(conteudo.contains("As parcelas são geradas pela plataforma após a formalização do contrato."));
		assertTrue(conteudo.contains("O credor aceitou a proposta na plataforma e o contrato foi formalizado automaticamente."));
		assertTrue(conteudo.contains("Natureza: documento eletrônico gerado automaticamente pela plataforma LoanFlow"));
		assertTrue(conteudo.contains("A operação é simulada e registrada pela plataforma."));
		assertFalse(conteudo.toLowerCase().contains("acadêmic"));
		assertFalse(conteudo.toLowerCase().contains("academic"));
	}

	@Test
	void gerarDeveExigirPropostaAceitaOuAprovada() {
		var credorUsuario = usuario(15L, Role.CREDOR);
		var solicitanteUsuario = usuario(25L, Role.SOLICITANTE);
		var proposta = proposta(105L, solicitanteUsuario, credorUsuario, PropostaStatus.AGUARDANDO_ACEITE);
		when(usuarioService.usuarioAtual()).thenReturn(credorUsuario);
		when(propostaRepository.findById(105L)).thenReturn(Optional.of(proposta));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> contratoService.gerar(105L, "127.0.0.1")
		);

		assertTrue(exception.getMessage().contains("aceita ou aprovada"));
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
		proposta.setNumeroProposta("PPT-2026-%06d".formatted(id));
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
}
