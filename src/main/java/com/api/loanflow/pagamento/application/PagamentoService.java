package com.api.loanflow.pagamento.application;

import com.api.loanflow.auditoria.domain.AuditoriaAcao;
import com.api.loanflow.auditoria.application.AuditoriaService;
import com.api.loanflow.shared.exception.RecursoNaoEncontradoException;
import com.api.loanflow.shared.exception.RegraNegocioException;
import com.api.loanflow.notificacao.domain.TipoNotificacao;
import com.api.loanflow.notificacao.application.NotificacaoService;
import com.api.loanflow.pagamento.api.dto.PagamentoResponse;
import com.api.loanflow.pagamento.api.dto.RegistrarPagamentoRequest;
import com.api.loanflow.pagamento.domain.Pagamento;
import com.api.loanflow.pagamento.domain.PagamentoStatus;
import com.api.loanflow.pagamento.infrastructure.persistence.PagamentoRepository;
import com.api.loanflow.parcela.domain.ParcelaStatus;
import com.api.loanflow.parcela.application.ParcelaService;
import com.api.loanflow.usuario.domain.Role;
import com.api.loanflow.usuario.domain.Usuario;
import com.api.loanflow.usuario.application.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PagamentoService {
	private final PagamentoRepository pagamentoRepository;
	private final ParcelaService parcelaService;
	private final UsuarioService usuarioService;
	private final AuditoriaService auditoriaService;
	private final NotificacaoService notificacaoService;

	public PagamentoService(
		PagamentoRepository pagamentoRepository,
		ParcelaService parcelaService,
		UsuarioService usuarioService,
		AuditoriaService auditoriaService,
		NotificacaoService notificacaoService
	) {
		this.pagamentoRepository = pagamentoRepository;
		this.parcelaService = parcelaService;
		this.usuarioService = usuarioService;
		this.auditoriaService = auditoriaService;
		this.notificacaoService = notificacaoService;
	}

	@Transactional
	public PagamentoResponse registrar(Long parcelaId, RegistrarPagamentoRequest request, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var parcela = parcelaService.buscarComPermissao(parcelaId);
		exigirSolicitanteOuAdmin(usuario, parcela.getContrato().getProposta().getSolicitante().getUsuario().getId());
		if (parcela.getStatus() == ParcelaStatus.PAGA) {
			throw new RegraNegocioException("Parcela já está paga.");
		}
		var saldoPendente = parcela.getValorPrevisto().subtract(parcela.getValorPagoAcumulado());
		if (request.valorPago().compareTo(saldoPendente) > 0) {
			throw new RegraNegocioException("Pagamento não pode exceder o saldo pendente da parcela.");
		}

		var pagamento = new Pagamento();
		pagamento.setParcela(parcela);
		pagamento.setUsuarioRegistrador(usuario);
		pagamento.setValorPago(request.valorPago());
		pagamento.setFormaPagamento(request.formaPagamento());
		pagamento.setComprovante(request.comprovante());
		pagamento = pagamentoRepository.save(pagamento);

		var novoValorPago = parcela.getValorPagoAcumulado().add(request.valorPago());
		parcela.setValorPagoAcumulado(novoValorPago);
		parcela.setStatus(novoValorPago.compareTo(parcela.getValorPrevisto()) >= 0 ? ParcelaStatus.PAGA : ParcelaStatus.PARCIALMENTE_PAGA);

		auditoriaService.registrar(usuario, AuditoriaAcao.REGISTRAR_PAGAMENTO, "Parcela", parcela.getId(), "Pagamento manual registrado.", ipOrigem);
		var credor = parcela.getContrato().getProposta().getCredor();
		if (credor != null) {
			notificacaoService.criar(credor.getUsuario(), TipoNotificacao.PAGAMENTO, "Pagamento registrado em uma parcela.", "Parcela", parcela.getId());
		}
		return PagamentoResponse.from(pagamento);
	}

	@Transactional(readOnly = true)
	public List<PagamentoResponse> listarPorParcela(Long parcelaId) {
		parcelaService.buscarComPermissao(parcelaId);
		return pagamentoRepository.findByParcelaIdOrderByDataHoraPagamentoAsc(parcelaId).stream()
			.map(PagamentoResponse::from)
			.toList();
	}

	@Transactional
	public PagamentoResponse cancelar(Long parcelaId, Long pagamentoId, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var parcela = parcelaService.buscarComPermissao(parcelaId);
		exigirSolicitanteOuAdmin(usuario, parcela.getContrato().getProposta().getSolicitante().getUsuario().getId());
		var pagamento = pagamentoRepository.findById(pagamentoId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado."));
		if (!pagamento.getParcela().getId().equals(parcela.getId())) {
			throw new RecursoNaoEncontradoException("Pagamento não encontrado.");
		}
		if (pagamento.getStatus() == PagamentoStatus.CANCELADO) {
			throw new RegraNegocioException("Pagamento já está cancelado.");
		}

		pagamento.setStatus(PagamentoStatus.CANCELADO);
		var novoValorPago = parcela.getValorPagoAcumulado().subtract(pagamento.getValorPago());
		if (novoValorPago.signum() < 0) {
			novoValorPago = java.math.BigDecimal.ZERO;
		}
		parcela.setValorPagoAcumulado(novoValorPago);
		if (novoValorPago.signum() == 0) {
			parcela.setStatus(ParcelaStatus.ABERTA);
		} else if (novoValorPago.compareTo(parcela.getValorPrevisto()) >= 0) {
			parcela.setStatus(ParcelaStatus.PAGA);
		} else {
			parcela.setStatus(ParcelaStatus.PARCIALMENTE_PAGA);
		}

		auditoriaService.registrar(usuario, AuditoriaAcao.CANCELAR, "Pagamento", pagamento.getId(), "Pagamento manual cancelado.", ipOrigem);
		return PagamentoResponse.from(pagamento);
	}

	private void exigirSolicitanteOuAdmin(Usuario usuario, Long solicitanteUsuarioId) {
		if (usuario.possuiPapel(Role.ADMIN)) {
			return;
		}
		if (!usuario.getId().equals(solicitanteUsuarioId)) {
			throw new RegraNegocioException("Apenas o solicitante responsável ou admin pode registrar pagamento.");
		}
	}
}
