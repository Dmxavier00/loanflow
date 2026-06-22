package com.api.loanflow.proposta.api;

import com.api.loanflow.proposta.api.dto.AtualizarPropostaRequest;
import com.api.loanflow.proposta.api.dto.CriarPropostaRequest;
import com.api.loanflow.proposta.api.dto.PropostaResponse;
import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import com.api.loanflow.proposta.aplicacao.PropostaService;
import com.api.loanflow.proposta.dominio.PropostaStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/propostas")
public class PropostaController {
	private final PropostaService propostaService;

	public PropostaController(PropostaService propostaService) {
		this.propostaService = propostaService;
	}

	@PostMapping
	@PreAuthorize("hasRole('SOLICITANTE')")
	public PropostaResponse criar(@Valid @RequestBody CriarPropostaRequest request, HttpServletRequest servletRequest) {
		return propostaService.criar(request, servletRequest.getRemoteAddr());
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SOLICITANTE','CREDOR','ADMIN')")
	public List<PropostaResponse> listar(
		@RequestParam(required = false) PropostaStatus status,
		@RequestParam(required = false) CategoriaFinalidade categoriaFinalidade,
		@RequestParam(required = false) BigDecimal valorMinimo,
		@RequestParam(required = false) BigDecimal valorMaximo,
		@RequestParam(required = false) String finalidade
	) {
		return propostaService.listarComFiltros(status, categoriaFinalidade, valorMinimo, valorMaximo, finalidade);
	}

	@GetMapping("/minhas")
	@PreAuthorize("hasRole('SOLICITANTE')")
	public List<PropostaResponse> minhas() {
		return propostaService.minhas();
	}

	@GetMapping("/analise")
	@PreAuthorize("hasRole('CREDOR')")
	public List<PropostaResponse> paraAnalise() {
		return propostaService.paraAnalise();
	}

	@GetMapping("/aguardando-aceite")
	@PreAuthorize("hasRole('CREDOR')")
	public List<PropostaResponse> aguardandoAceite() {
		return propostaService.aguardandoAceite();
	}

	@GetMapping("/aceitas")
	@PreAuthorize("hasRole('CREDOR')")
	public List<PropostaResponse> aceitasPeloCredor() {
		return propostaService.aceitasPeloCredor();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SOLICITANTE','CREDOR','ADMIN')")
	public PropostaResponse detalhar(@PathVariable Long id) {
		return propostaService.detalhar(id);
	}

	@GetMapping("/status/{status}")
	@PreAuthorize("hasAnyRole('SOLICITANTE','CREDOR','ADMIN')")
	public List<PropostaResponse> listarPorStatus(@PathVariable PropostaStatus status) {
		return propostaService.listarPorStatus(status);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('SOLICITANTE')")
	public PropostaResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarPropostaRequest request, HttpServletRequest servletRequest) {
		return propostaService.atualizar(id, request, servletRequest.getRemoteAddr());
	}

	@PostMapping("/{id}/submeter")
	@PreAuthorize("hasRole('SOLICITANTE')")
	public PropostaResponse submeter(@PathVariable Long id, HttpServletRequest servletRequest) {
		return propostaService.submeter(id, servletRequest.getRemoteAddr());
	}

	@PostMapping("/{id}/iniciar-analise")
	@PreAuthorize("hasRole('CREDOR')")
	public PropostaResponse iniciarAnalise(@PathVariable Long id, HttpServletRequest servletRequest) {
		return propostaService.iniciarAnalise(id, servletRequest.getRemoteAddr());
	}

	@PostMapping("/{id}/aceitar")
	@PreAuthorize("hasRole('CREDOR')")
	public PropostaResponse aceitar(@PathVariable Long id, HttpServletRequest servletRequest) {
		return propostaService.aceitar(id, servletRequest.getRemoteAddr());
	}

	@PostMapping("/{id}/aprovar")
	@PreAuthorize("hasRole('CREDOR')")
	public PropostaResponse aprovar(@PathVariable Long id, HttpServletRequest servletRequest) {
		return propostaService.aprovar(id, servletRequest.getRemoteAddr());
	}

	@PostMapping("/{id}/rejeitar")
	@PreAuthorize("hasRole('CREDOR')")
	public PropostaResponse rejeitar(@PathVariable Long id, HttpServletRequest servletRequest) {
		return propostaService.rejeitar(id, servletRequest.getRemoteAddr());
	}

	@PostMapping("/{id}/cancelar")
	@PreAuthorize("hasAnyRole('SOLICITANTE','ADMIN')")
	public PropostaResponse cancelar(@PathVariable Long id, HttpServletRequest servletRequest) {
		return propostaService.cancelar(id, servletRequest.getRemoteAddr());
	}
}
