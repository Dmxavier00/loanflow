package com.api.loanflow.contrato.api;

import com.api.loanflow.contrato.api.dto.ContratoResponse;
import com.api.loanflow.contrato.aplicacao.ContratoService;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contratos")
public class ContratoController {
	private final ContratoService contratoService;

	public ContratoController(ContratoService contratoService) {
		this.contratoService = contratoService;
	}

	@PostMapping("/proposta/{propostaId}/gerar")
	@PreAuthorize("hasAnyRole('CREDOR','ADMIN')")
	public ContratoResponse gerar(@PathVariable Long propostaId, HttpServletRequest servletRequest) {
		return contratoService.gerar(propostaId, servletRequest.getRemoteAddr());
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SOLICITANTE','CREDOR','ADMIN')")
	public List<ContratoResponse> listar(
		@RequestParam(required = false) ContratoStatus status,
		@RequestParam(required = false) String numeroContrato,
		@RequestParam(required = false) CategoriaFinalidade categoriaFinalidade,
		@RequestParam(required = false) String nomeContraparte
	) {
		return contratoService.listarComFiltros(status, numeroContrato, categoriaFinalidade, nomeContraparte);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SOLICITANTE','CREDOR','ADMIN')")
	public ContratoResponse detalhar(@PathVariable Long id) {
		return contratoService.detalhar(id);
	}

	@GetMapping("/{id}/download")
	@PreAuthorize("hasAnyRole('SOLICITANTE','CREDOR','ADMIN')")
	public ResponseEntity<byte[]> download(@PathVariable Long id) {
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contrato-" + id + ".pdf")
			.contentType(MediaType.APPLICATION_PDF)
			.body(contratoService.baixarPdf(id));
	}

	@PostMapping("/{id}/cancelar")
	@PreAuthorize("hasAnyRole('CREDOR','ADMIN')")
	public ContratoResponse cancelar(@PathVariable Long id, HttpServletRequest servletRequest) {
		return contratoService.cancelar(id, servletRequest.getRemoteAddr());
	}
}
