package com.api.loanflow.proposta.aplicacao;

import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.parcela.dominio.Parcela;
import com.api.loanflow.parcela.dominio.ParcelaStatus;
import com.api.loanflow.parcela.infraestrutura.persistencia.ParcelaRepository;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.usuario.dominio.SolicitanteCredito;
import com.api.loanflow.usuario.dominio.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PoliticaCreditoServiceTest {

	@Mock
	private ParcelaRepository parcelaRepository;

	private PoliticaCreditoService politicaCreditoService;

	@BeforeEach
	void setUp() {
		politicaCreditoService = new PoliticaCreditoService(parcelaRepository);
	}

	@Test
	void deveExigirRendaMensalInformada() {
		var solicitante = solicitante(1L, null);

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> politicaCreditoService.validarNovaContratacao(solicitante, new BigDecimal("1000.00"), new BigDecimal("2.50"), 10)
		);

		assertEquals("Solicitante precisa informar renda mensal válida antes de contratar empréstimo.", exception.getMessage());
	}

	@Test
	void deveBloquearSolicitanteComParcelasEmAtraso() {
		var solicitante = solicitante(2L, new BigDecimal("5000.00"));
		when(parcelaRepository.existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusIn(
			eq(ContratoStatus.FORMALIZADO),
			eq(2L),
			anyCollection()
		)).thenReturn(true);

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> politicaCreditoService.validarNovaContratacao(solicitante, new BigDecimal("1000.00"), new BigDecimal("2.50"), 10)
		);

		assertEquals("Solicitante possui parcelas vencidas ou em atraso e não pode contratar novo empréstimo.", exception.getMessage());
	}

	@Test
	void deveBloquearQuandoAtingeLimiteDeContratosAtivos() {
		var solicitante = solicitante(3L, new BigDecimal("5000.00"));
		when(parcelaRepository.existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusIn(
			eq(ContratoStatus.FORMALIZADO),
			eq(3L),
			anyCollection()
		)).thenReturn(false);
		when(parcelaRepository.existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNotAndDataVencimentoBefore(
			eq(ContratoStatus.FORMALIZADO),
			eq(3L),
			eq(ParcelaStatus.PAGA),
			any(LocalDate.class)
		)).thenReturn(false);
		when(parcelaRepository.countDistinctContratoIdByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNot(
			ContratoStatus.FORMALIZADO,
			3L,
			ParcelaStatus.PAGA
		)).thenReturn(2L);

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> politicaCreditoService.validarNovaContratacao(solicitante, new BigDecimal("1000.00"), new BigDecimal("2.50"), 10)
		);

		assertEquals("Solicitante atingiu o limite de 2 contratos ativos simultâneos.", exception.getMessage());
	}

	@Test
	void deveBloquearQuandoComprometimentoExcedeTrintaPorCentoDaRenda() {
		var solicitante = solicitante(4L, new BigDecimal("1000.00"));
		when(parcelaRepository.existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusIn(
			eq(ContratoStatus.FORMALIZADO),
			eq(4L),
			anyCollection()
		)).thenReturn(false);
		when(parcelaRepository.existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNotAndDataVencimentoBefore(
			eq(ContratoStatus.FORMALIZADO),
			eq(4L),
			eq(ParcelaStatus.PAGA),
			any(LocalDate.class)
		)).thenReturn(false);
		when(parcelaRepository.countDistinctContratoIdByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNot(
			ContratoStatus.FORMALIZADO,
			4L,
			ParcelaStatus.PAGA
		)).thenReturn(1L);
		when(parcelaRepository.findByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNotOrderByContratoIdAscNumeroAsc(
			ContratoStatus.FORMALIZADO,
			4L,
			ParcelaStatus.PAGA
		)).thenReturn(List.of(parcelaPendente(400L, new BigDecimal("180.00"), BigDecimal.ZERO)));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> politicaCreditoService.validarNovaContratacao(solicitante, new BigDecimal("1500.00"), new BigDecimal("10.00"), 6)
		);

		assertEquals("Comprometimento mensal excede 30% da renda informada para o solicitante.", exception.getMessage());
	}

	@Test
	void devePermitirQuandoSolicitanteEstaDentroDaPolitica() {
		var solicitante = solicitante(5L, new BigDecimal("4000.00"));
		when(parcelaRepository.existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusIn(
			eq(ContratoStatus.FORMALIZADO),
			eq(5L),
			anyCollection()
		)).thenReturn(false);
		when(parcelaRepository.existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNotAndDataVencimentoBefore(
			eq(ContratoStatus.FORMALIZADO),
			eq(5L),
			eq(ParcelaStatus.PAGA),
			any(LocalDate.class)
		)).thenReturn(false);
		when(parcelaRepository.countDistinctContratoIdByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNot(
			ContratoStatus.FORMALIZADO,
			5L,
			ParcelaStatus.PAGA
		)).thenReturn(1L);
		when(parcelaRepository.findByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNotOrderByContratoIdAscNumeroAsc(
			ContratoStatus.FORMALIZADO,
			5L,
			ParcelaStatus.PAGA
		)).thenReturn(List.of(parcelaPendente(500L, new BigDecimal("200.00"), new BigDecimal("50.00"))));

		assertDoesNotThrow(
			() -> politicaCreditoService.validarNovaContratacao(solicitante, new BigDecimal("900.00"), new BigDecimal("2.00"), 6)
		);
	}

	private SolicitanteCredito solicitante(Long usuarioId, BigDecimal rendaMensal) {
		var usuario = new Usuario();
		ReflectionTestUtils.setField(usuario, "id", usuarioId);
		var solicitante = new SolicitanteCredito();
		solicitante.setUsuario(usuario);
		solicitante.setRendaMensal(rendaMensal);
		return solicitante;
	}

	private Parcela parcelaPendente(Long contratoId, BigDecimal valorPrevisto, BigDecimal valorPagoAcumulado) {
		var contrato = new Contrato();
		ReflectionTestUtils.setField(contrato, "id", contratoId);
		var parcela = new Parcela();
		parcela.setContrato(contrato);
		parcela.setValorPrevisto(valorPrevisto);
		parcela.setValorPagoAcumulado(valorPagoAcumulado);
		parcela.setStatus(ParcelaStatus.ABERTA);
		return parcela;
	}
}
