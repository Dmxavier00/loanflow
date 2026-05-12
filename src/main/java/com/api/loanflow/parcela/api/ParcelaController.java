package com.api.loanflow.parcela.api;

import com.api.loanflow.parcela.api.dto.ParcelaResponse;
import com.api.loanflow.parcela.application.ParcelaService;
import com.api.loanflow.parcela.domain.ParcelaStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping
public class ParcelaController {
	private final ParcelaService parcelaService;

	public ParcelaController(ParcelaService parcelaService) {
		this.parcelaService = parcelaService;
	}

	@GetMapping("/parcelas")
	@PreAuthorize("hasAnyRole('SOLICITANTE','CREDOR','ADMIN')")
	public List<ParcelaResponse> listar(
		@RequestParam(required = false) String numeroContrato,
		@RequestParam(required = false) ParcelaStatus status,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimentoInicio,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimentoFim
	) {
		return parcelaService.listarComFiltros(numeroContrato, status, dataVencimentoInicio, dataVencimentoFim);
	}

	@GetMapping("/contratos/{contratoId}/parcelas")
	@PreAuthorize("hasRole('ADMIN')")
	public List<ParcelaResponse> listarPorContrato(@PathVariable Long contratoId) {
		return parcelaService.listarPorContrato(contratoId);
	}

	@GetMapping("/parcelas/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ParcelaResponse detalhar(@PathVariable Long id) {
		return parcelaService.detalhar(id);
	}

	@PostMapping("/admin/parcelas/marcar-atrasadas")
	@PreAuthorize("hasRole('ADMIN')")
	public List<ParcelaResponse> marcarAtrasadas(HttpServletRequest servletRequest) {
		return parcelaService.marcarAtrasadas(servletRequest.getRemoteAddr());
	}
}
