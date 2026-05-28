package com.api.loanflow.compartilhado.aplicacao;

import com.api.loanflow.compartilhado.criptografia.HashService;
import com.api.loanflow.contrato.aplicacao.ContratoConteudoService;
import com.api.loanflow.contrato.aplicacao.PdfContratoService;
import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.contrato.infraestrutura.persistencia.ContratoRepository;
import com.api.loanflow.parcela.aplicacao.ParcelaService;
import com.api.loanflow.proposta.dominio.Proposta;
import com.api.loanflow.proposta.infraestrutura.persistencia.PropostaRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilidadeNumeracaoStartupServiceTest {

	@Mock
	private PropostaRepository propostaRepository;
	@Mock
	private ContratoRepository contratoRepository;
	@Mock
	private ContratoConteudoService contratoConteudoService;
	@Mock
	private HashService hashService;
	@Mock
	private PdfContratoService pdfContratoService;
	@Mock
	private ParcelaService parcelaService;

	private CompatibilidadeNumeracaoStartupService service;

	@BeforeEach
	void setUp() {
		service = new CompatibilidadeNumeracaoStartupService(
			propostaRepository,
			contratoRepository,
			new NumeroNegocioService(),
			contratoConteudoService,
			hashService,
			pdfContratoService,
			parcelaService
		);
	}

	@Test
	void compatibilizarNumeracaoLegadaDeveAtualizarPropostasEContratos() {
		var proposta = new Proposta();
		ReflectionTestUtils.setField(proposta, "id", 12L);
		ReflectionTestUtils.setField(proposta, "dataCriacao", LocalDateTime.of(2026, 1, 5, 9, 0));
		proposta.setNumeroProposta("PR-2026-000012");

		var contrato = new Contrato();
		ReflectionTestUtils.setField(contrato, "id", 34L);
		ReflectionTestUtils.setField(contrato, "dataGeracao", LocalDateTime.of(2026, 1, 7, 14, 30));
		contrato.setProposta(proposta);
		contrato.setNumeroContrato("LF-2026-000034");
		contrato.setStatus(ContratoStatus.FORMALIZADO);
		contrato.setConteudoSnapshot("conteudo-legado");
		contrato.setHashDocumento("hash-legado");
		contrato.setHashPdfEmitido("hash-pdf-legado");
		contrato.setPdfPath("legacy.pdf");
		contrato.setDataFormalizacao(LocalDateTime.of(2026, 1, 7, 14, 31));

		when(propostaRepository.findAll(any(Sort.class))).thenReturn(List.of(proposta));
		when(contratoRepository.findAll(any(Sort.class))).thenReturn(List.of(contrato));
		when(contratoConteudoService.montarConteudoContrato(any(String.class), any(Proposta.class), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenAnswer(invocation -> invocation.getArgument(0, String.class) + "|" + invocation.getArgument(1, Proposta.class).getNumeroProposta());
		when(hashService.sha256(any(String.class)))
			.thenAnswer(invocation -> "sha(" + invocation.getArgument(0, String.class) + ")");
		when(hashService.sha256(any(byte[].class)))
			.thenAnswer(invocation -> "sha-bytes(" + new String(invocation.getArgument(0, byte[].class), StandardCharsets.UTF_8) + ")");
		when(pdfContratoService.gerarContratoPdf(any(String.class), any(String.class))).thenReturn("ctr.pdf");
		when(pdfContratoService.lerPdf("ctr.pdf")).thenReturn("pdf-ctr".getBytes(StandardCharsets.UTF_8));

		service.compatibilizarNumeracaoLegada();

		assertEquals("PPT-2026-000012", proposta.getNumeroProposta());
		assertEquals("CTR-2026-000034", contrato.getNumeroContrato());
		assertEquals("CTR-2026-000034|PPT-2026-000012", contrato.getConteudoSnapshot());
		assertEquals("sha(CTR-2026-000034|PPT-2026-000012)", contrato.getHashDocumento());
		assertEquals("sha-bytes(pdf-ctr)", contrato.getHashPdfEmitido());
		assertEquals("ctr.pdf", contrato.getPdfPath());

		verify(pdfContratoService).excluirPdfSeExistir("legacy.pdf");
		verify(parcelaService).gerarParcelas(contrato, null, "startup");
	}
}
