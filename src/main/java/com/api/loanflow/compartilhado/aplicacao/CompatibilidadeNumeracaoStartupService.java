package com.api.loanflow.compartilhado.aplicacao;

import com.api.loanflow.compartilhado.criptografia.HashService;
import com.api.loanflow.contrato.aplicacao.ContratoConteudoService;
import com.api.loanflow.contrato.aplicacao.PdfContratoService;
import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.contrato.infraestrutura.persistencia.ContratoRepository;
import com.api.loanflow.parcela.aplicacao.ParcelaService;
import com.api.loanflow.proposta.infraestrutura.persistencia.PropostaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CompatibilidadeNumeracaoStartupService {
	private static final Logger logger = LoggerFactory.getLogger(CompatibilidadeNumeracaoStartupService.class);

	private final PropostaRepository propostaRepository;
	private final ContratoRepository contratoRepository;
	private final NumeroNegocioService numeroNegocioService;
	private final ContratoConteudoService contratoConteudoService;
	private final HashService hashService;
	private final PdfContratoService pdfContratoService;
	private final ParcelaService parcelaService;

	public CompatibilidadeNumeracaoStartupService(
		PropostaRepository propostaRepository,
		ContratoRepository contratoRepository,
		NumeroNegocioService numeroNegocioService,
		ContratoConteudoService contratoConteudoService,
		HashService hashService,
		PdfContratoService pdfContratoService,
		ParcelaService parcelaService
	) {
		this.propostaRepository = propostaRepository;
		this.contratoRepository = contratoRepository;
		this.numeroNegocioService = numeroNegocioService;
		this.contratoConteudoService = contratoConteudoService;
		this.hashService = hashService;
		this.pdfContratoService = pdfContratoService;
		this.parcelaService = parcelaService;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void compatibilizarNumeracaoLegada() {
		int propostasAtualizadas = 0;
		for (var proposta : propostaRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))) {
			var numeroAtualizado = numeroNegocioService.gerarNumeroProposta(proposta);
			if (numeroAtualizado.equals(proposta.getNumeroProposta())
				&& numeroNegocioService.numeroPropostaEstaNoFormatoAtual(proposta.getNumeroProposta())) {
				continue;
			}
			proposta.setNumeroProposta(numeroAtualizado);
			propostasAtualizadas++;
		}

		int contratosAtualizados = 0;
		int contratosComParcelasCompatibilizadas = 0;
		for (var contrato : contratoRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))) {
			if (!numeroNegocioService.numeroContratoEstaNoFormatoAtual(contrato.getNumeroContrato())) {
				atualizarContrato(contrato);
				contratosAtualizados++;
			} else if (contrato.getStatus() == ContratoStatus.FORMALIZADO && contratoPrecisaAtualizarDocumento(contrato)) {
				atualizarContrato(contrato);
				contratosAtualizados++;
			}

			if (contrato.getStatus() == ContratoStatus.FORMALIZADO
				&& parcelaService.gerarParcelas(contrato, null, "startup")) {
				contratosComParcelasCompatibilizadas++;
			}
		}

		if (propostasAtualizadas > 0 || contratosAtualizados > 0 || contratosComParcelasCompatibilizadas > 0) {
			logger.info(
				"Compatibilizacao de numeracao concluida. Propostas atualizadas: {}. Contratos atualizados: {}. Contratos com parcelas compatibilizadas: {}.",
				propostasAtualizadas,
				contratosAtualizados,
				contratosComParcelasCompatibilizadas
			);
		}
	}

	private boolean contratoPrecisaAtualizarDocumento(Contrato contrato) {
		var dataGeracao = contrato.getDataGeracao();
		var dataFormalizacao = contrato.getDataFormalizacao() == null ? dataGeracao : contrato.getDataFormalizacao();
		var conteudoAtual = contratoConteudoService.montarConteudoContrato(
			contrato.getNumeroContrato(),
			contrato.getProposta(),
			dataGeracao,
			dataFormalizacao
		);
		return !hashService.sha256(conteudoAtual).equals(contrato.getHashDocumento());
	}

	private void atualizarContrato(Contrato contrato) {
		var numeroAtualizado = numeroNegocioService.gerarNumeroContrato(contrato);
		var dataGeracao = contrato.getDataGeracao();
		var dataFormalizacao = contrato.getDataFormalizacao() == null ? dataGeracao : contrato.getDataFormalizacao();
		var conteudo = contratoConteudoService.montarConteudoContrato(
			numeroAtualizado,
			contrato.getProposta(),
			dataGeracao,
			dataFormalizacao
		);
		var pdfLegado = contrato.getPdfPath();
		var hashDocumento = hashService.sha256(conteudo);
		var novoPdfPath = pdfContratoService.gerarContratoPdf(numeroAtualizado, conteudo);
		var hashPdfEmitido = hashService.sha256(pdfContratoService.lerPdf(novoPdfPath));

		contrato.setNumeroContrato(numeroAtualizado);
		contrato.setConteudoSnapshot(conteudo);
		contrato.setHashDocumento(hashDocumento);
		contrato.setPdfPath(novoPdfPath);
		contrato.setHashPdfEmitido(hashPdfEmitido);
		if (pdfLegado != null && !pdfLegado.equals(novoPdfPath)) {
			pdfContratoService.excluirPdfSeExistir(pdfLegado);
		}
	}
}
