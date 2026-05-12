package com.api.loanflow.admin.application;

import com.api.loanflow.admin.api.dto.AdminDashboardResponse;
import com.api.loanflow.auditoria.infrastructure.persistence.AuditoriaRepository;
import com.api.loanflow.contrato.domain.ContratoStatus;
import com.api.loanflow.contrato.infrastructure.persistence.ContratoRepository;
import com.api.loanflow.pagamento.infrastructure.persistence.PagamentoRepository;
import com.api.loanflow.parcela.domain.ParcelaStatus;
import com.api.loanflow.parcela.infrastructure.persistence.ParcelaRepository;
import com.api.loanflow.proposta.domain.PropostaStatus;
import com.api.loanflow.proposta.infrastructure.persistence.PropostaRepository;
import com.api.loanflow.usuario.infrastructure.persistence.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
	private final UsuarioRepository usuarioRepository;
	private final PropostaRepository propostaRepository;
	private final ContratoRepository contratoRepository;
	private final ParcelaRepository parcelaRepository;
	private final PagamentoRepository pagamentoRepository;
	private final AuditoriaRepository auditoriaRepository;

	public AdminDashboardService(
		UsuarioRepository usuarioRepository,
		PropostaRepository propostaRepository,
		ContratoRepository contratoRepository,
		ParcelaRepository parcelaRepository,
		PagamentoRepository pagamentoRepository,
		AuditoriaRepository auditoriaRepository
	) {
		this.usuarioRepository = usuarioRepository;
		this.propostaRepository = propostaRepository;
		this.contratoRepository = contratoRepository;
		this.parcelaRepository = parcelaRepository;
		this.pagamentoRepository = pagamentoRepository;
		this.auditoriaRepository = auditoriaRepository;
	}

	@Transactional(readOnly = true)
	public AdminDashboardResponse dashboard() {
		return new AdminDashboardResponse(
			usuarioRepository.count(),
			propostaRepository.count(),
			propostaRepository.countByStatusIn(java.util.List.of(
				PropostaStatus.ACEITA,
				PropostaStatus.EM_ANALISE,
				PropostaStatus.APROVADA
			)),
			propostaRepository.countByStatus(PropostaStatus.CONTRATADA),
			contratoRepository.countByStatus(ContratoStatus.FORMALIZADO),
			parcelaRepository.countByStatus(ParcelaStatus.ABERTA),
			parcelaRepository.countByStatus(ParcelaStatus.EM_ATRASO),
			pagamentoRepository.count(),
			auditoriaRepository.count()
		);
	}
}
