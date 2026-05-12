package com.api.loanflow.auditoria.api;

import com.api.loanflow.auditoria.api.dto.AuditoriaResponse;
import com.api.loanflow.auditoria.domain.AuditoriaAcao;
import com.api.loanflow.auditoria.infrastructure.persistence.AuditoriaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/auditorias")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {
	private final AuditoriaRepository auditoriaRepository;

	public AuditoriaController(AuditoriaRepository auditoriaRepository) {
		this.auditoriaRepository = auditoriaRepository;
	}

	@GetMapping
	public List<AuditoriaResponse> listar(
		@RequestParam(required = false) AuditoriaAcao acao,
		@RequestParam(required = false) String entidadeTipo,
		@RequestParam(required = false) Long entidadeId
	) {
		return auditoriaRepository.findAll().stream()
			.filter(auditoria -> acao == null || auditoria.getAcao() == acao)
			.filter(auditoria -> entidadeTipo == null || auditoria.getEntidadeTipo().equalsIgnoreCase(entidadeTipo))
			.filter(auditoria -> entidadeId == null || entidadeId.equals(auditoria.getEntidadeId()))
			.map(AuditoriaResponse::from)
			.toList();
	}
}
