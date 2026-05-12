package com.api.loanflow.proposta.aplicacao;

import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import com.api.loanflow.proposta.dominio.Proposta;
import com.api.loanflow.proposta.dominio.PropostaStatus;
import com.api.loanflow.proposta.infraestrutura.persistencia.PropostaRepository;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.usuario.dominio.Credor;
import com.api.loanflow.usuario.dominio.Role;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PoliticaCredorServiceTest {

	@Mock
	private PropostaRepository propostaRepository;

	private PoliticaCredorService politicaCredorService;

	@BeforeEach
	void setUp() {
		politicaCredorService = new PoliticaCredorService(propostaRepository);
	}

	@Test
	void deveBloquearQuandoSaldoDisponivelNaoFoiInformado() {
		var credor = credor(10L, BigDecimal.ZERO, 3);
		var proposta = proposta(100L, null, PropostaStatus.AGUARDANDO_ACEITE, new BigDecimal("500.00"));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> politicaCredorService.validarDisponibilidade(credor, proposta)
		);

		assertEquals("Credor precisa informar saldo disponível válido antes de assumir operações.", exception.getMessage());
	}

	@Test
	void deveBloquearQuandoLimiteDeOperacoesFoiAtingido() {
		var credor = credor(11L, new BigDecimal("10000.00"), 2);
		var proposta = proposta(101L, null, PropostaStatus.AGUARDANDO_ACEITE, new BigDecimal("600.00"));
		when(propostaRepository.countByCredorUsuarioIdAndStatusIn(eq(11L), anyCollection())).thenReturn(2L);

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> politicaCredorService.validarDisponibilidade(credor, proposta)
		);

		assertEquals("Credor atingiu o limite máximo de operações ativas.", exception.getMessage());
	}

	@Test
	void deveBloquearQuandoSaldoLivreNaoCobreNovaProposta() {
		var credor = credor(12L, new BigDecimal("1000.00"), 4);
		var proposta = proposta(102L, null, PropostaStatus.AGUARDANDO_ACEITE, new BigDecimal("500.00"));
		when(propostaRepository.countByCredorUsuarioIdAndStatusIn(eq(12L), anyCollection())).thenReturn(1L);
		when(propostaRepository.findByCredorUsuarioIdAndStatusInOrderByDataCriacaoDesc(eq(12L), anyCollection()))
			.thenReturn(List.of(proposta(202L, credor, PropostaStatus.APROVADA, new BigDecimal("800.00"))));

		var exception = assertThrows(
			RegraNegocioException.class,
			() -> politicaCredorService.validarDisponibilidade(credor, proposta)
		);

		assertEquals("Credor não possui saldo disponível suficiente para esta proposta.", exception.getMessage());
	}

	@Test
	void devePermitirQuandoSaldoELimiteEstaoDisponiveis() {
		var credor = credor(13L, new BigDecimal("5000.00"), 3);
		var proposta = proposta(103L, credor, PropostaStatus.EM_ANALISE, new BigDecimal("1200.00"));
		when(propostaRepository.countByCredorUsuarioIdAndStatusIn(eq(13L), anyCollection())).thenReturn(2L);
		when(propostaRepository.findByCredorUsuarioIdAndStatusInOrderByDataCriacaoDesc(eq(13L), anyCollection()))
			.thenReturn(List.of(
				proposta,
				proposta(203L, credor, PropostaStatus.ACEITA, new BigDecimal("1000.00"))
			));

		assertDoesNotThrow(() -> politicaCredorService.validarDisponibilidade(credor, proposta));
	}

	private Credor credor(Long usuarioId, BigDecimal saldoDisponivelSimulado, Integer limiteOperacoes) {
		var usuario = new Usuario();
		ReflectionTestUtils.setField(usuario, "id", usuarioId);
		usuario.setPapel(Role.CREDOR);

		var credor = new Credor();
		ReflectionTestUtils.setField(credor, "id", usuarioId + 1000);
		credor.setUsuario(usuario);
		credor.setSaldoDisponivelSimulado(saldoDisponivelSimulado);
		credor.setLimiteOperacoes(limiteOperacoes);
		return credor;
	}

	private Proposta proposta(Long id, Credor credor, PropostaStatus status, BigDecimal valorSolicitado) {
		var solicitanteUsuario = new Usuario();
		ReflectionTestUtils.setField(solicitanteUsuario, "id", id + 3000);
		solicitanteUsuario.setPapel(Role.SOLICITANTE);

		var solicitante = new SolicitanteCredito();
		ReflectionTestUtils.setField(solicitante, "id", id + 4000);
		solicitante.setUsuario(solicitanteUsuario);

		var proposta = new Proposta();
		ReflectionTestUtils.setField(proposta, "id", id);
		proposta.setSolicitante(solicitante);
		proposta.setCredor(credor);
		proposta.setStatus(status);
		proposta.setValorSolicitado(valorSolicitado);
		proposta.setTaxaJuros(new BigDecimal("2.50"));
		proposta.setPrazoMeses(12);
		proposta.setFinalidade("Teste de politica do credor");
		proposta.setCategoriaFinalidade(CategoriaFinalidade.OUTRA);
		proposta.setDescricaoDetalhada("Validacao de saldo e limite do credor.");
		proposta.setDataExpiracao(LocalDate.now().plusDays(7));
		return proposta;
	}
}
