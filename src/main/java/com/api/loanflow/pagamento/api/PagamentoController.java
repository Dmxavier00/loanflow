package com.api.loanflow.pagamento.api;

import com.api.loanflow.pagamento.api.dto.PagamentoResponse;
import com.api.loanflow.pagamento.api.dto.RegistrarPagamentoRequest;
import com.api.loanflow.pagamento.application.PagamentoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/parcelas/{parcelaId}/pagamentos")
public class PagamentoController {
	private final PagamentoService pagamentoService;

	public PagamentoController(PagamentoService pagamentoService) {
		this.pagamentoService = pagamentoService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SOLICITANTE','ADMIN')")
	public PagamentoResponse registrar(
		@PathVariable Long parcelaId,
		@Valid @RequestBody RegistrarPagamentoRequest request,
		HttpServletRequest servletRequest
	) {
		return pagamentoService.registrar(parcelaId, request, servletRequest.getRemoteAddr());
	}

	@GetMapping
	public List<PagamentoResponse> listar(@PathVariable Long parcelaId) {
		return pagamentoService.listarPorParcela(parcelaId);
	}

	@PostMapping("/{pagamentoId}/cancelar")
	@PreAuthorize("hasAnyRole('SOLICITANTE','ADMIN')")
	public PagamentoResponse cancelar(
		@PathVariable Long parcelaId,
		@PathVariable Long pagamentoId,
		HttpServletRequest servletRequest
	) {
		return pagamentoService.cancelar(parcelaId, pagamentoId, servletRequest.getRemoteAddr());
	}
}
