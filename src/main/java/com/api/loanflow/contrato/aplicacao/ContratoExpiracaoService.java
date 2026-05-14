package com.api.loanflow.contrato.aplicacao;

import com.api.loanflow.auditoria.aplicacao.AuditoriaService;
import com.api.loanflow.auditoria.dominio.AuditoriaAcao;
import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.contrato.dominio.EventoAssinaturaTipo;
import com.api.loanflow.contrato.infraestrutura.persistencia.ContratoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;

@Service
public class ContratoExpiracaoService {
	private static final String DEFAULT_REASON = "Prazo limite para assinatura encerrado.";

	private final ContratoRepository contratoRepository;
	private final AuditoriaService auditoriaService;
	private final EventoAssinaturaService eventoAssinaturaService;

	public ContratoExpiracaoService(
		ContratoRepository contratoRepository,
		AuditoriaService auditoriaService,
		EventoAssinaturaService eventoAssinaturaService
	) {
		this.contratoRepository = contratoRepository;
		this.auditoriaService = auditoriaService;
		this.eventoAssinaturaService = eventoAssinaturaService;
	}

	@Transactional
	public void expirarPendentes() {
		var agora = LocalDateTime.now();
		var contratos = contratoRepository.findByStatusInAndDataExpiracaoAssinaturaBefore(
			EnumSet.of(ContratoStatus.AGUARDANDO_ASSINATURAS, ContratoStatus.ASSINADO_PARCIALMENTE),
			agora
		);
		for (Contrato contrato : contratos) {
			expirarSeNecessario(contrato, agora);
		}
	}

	@Scheduled(fixedDelay = 60000L, initialDelay = 60000L)
	public void expirarPendentesAgendado() {
		expirarPendentes();
	}

	@Transactional
	public boolean expirarSeNecessario(Contrato contrato) {
		return expirarSeNecessario(contrato, LocalDateTime.now());
	}

	private boolean expirarSeNecessario(Contrato contrato, LocalDateTime agora) {
		if (!contrato.estaExpirado(agora)) {
			return false;
		}

		contrato.marcarExpirado(DEFAULT_REASON, agora);
		auditoriaService.registrar(null, AuditoriaAcao.ATUALIZAR, "Contrato", contrato.getId(), DEFAULT_REASON, null);
		eventoAssinaturaService.registrar(
			contrato,
			null,
			EventoAssinaturaTipo.CONTRATO_EXPIRADO,
			DEFAULT_REASON,
			null,
			null
		);
		return true;
	}
}
