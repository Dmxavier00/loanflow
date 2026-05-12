package com.api.loanflow.admin.application;

import com.api.loanflow.auditoria.infrastructure.persistence.AuditoriaRepository;
import com.api.loanflow.contrato.domain.ContratoStatus;
import com.api.loanflow.contrato.infrastructure.persistence.ContratoRepository;
import com.api.loanflow.pagamento.infrastructure.persistence.PagamentoRepository;
import com.api.loanflow.parcela.domain.ParcelaStatus;
import com.api.loanflow.parcela.infrastructure.persistence.ParcelaRepository;
import com.api.loanflow.proposta.domain.PropostaStatus;
import com.api.loanflow.proposta.infrastructure.persistence.PropostaRepository;
import com.api.loanflow.usuario.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private PropostaRepository propostaRepository;
	@Mock
	private ContratoRepository contratoRepository;
	@Mock
	private ParcelaRepository parcelaRepository;
	@Mock
	private PagamentoRepository pagamentoRepository;
	@Mock
	private AuditoriaRepository auditoriaRepository;

	private AdminDashboardService adminDashboardService;

	@BeforeEach
	void setUp() {
		adminDashboardService = new AdminDashboardService(
			usuarioRepository,
			propostaRepository,
			contratoRepository,
			parcelaRepository,
			pagamentoRepository,
			auditoriaRepository
		);
	}

	@Test
	void dashboardDeveContarPropostasEmCarteiraIncluindoEmAnalise() {
		when(usuarioRepository.count()).thenReturn(12L);
		when(propostaRepository.count()).thenReturn(20L);
		when(propostaRepository.countByStatusIn(List.of(
			PropostaStatus.ACEITA,
			PropostaStatus.EM_ANALISE,
			PropostaStatus.APROVADA
		))).thenReturn(7L);
		when(propostaRepository.countByStatus(PropostaStatus.CONTRATADA)).thenReturn(3L);
		when(contratoRepository.countByStatus(ContratoStatus.FORMALIZADO)).thenReturn(5L);
		when(parcelaRepository.countByStatus(ParcelaStatus.ABERTA)).thenReturn(14L);
		when(parcelaRepository.countByStatus(ParcelaStatus.EM_ATRASO)).thenReturn(2L);
		when(pagamentoRepository.count()).thenReturn(11L);
		when(auditoriaRepository.count()).thenReturn(30L);

		var response = adminDashboardService.dashboard();

		assertEquals(7L, response.propostasAprovadas());
		verify(propostaRepository).countByStatusIn(List.of(
			PropostaStatus.ACEITA,
			PropostaStatus.EM_ANALISE,
			PropostaStatus.APROVADA
		));
	}
}
