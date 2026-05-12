package com.api.loanflow.usuario.api;

import com.api.loanflow.usuario.api.dto.AlterarSenhaRequest;
import com.api.loanflow.usuario.api.dto.AtualizarContaBancariaRequest;
import com.api.loanflow.usuario.api.dto.AtualizarDadosFinanceirosRequest;
import com.api.loanflow.usuario.api.dto.AtualizarPerfilRequest;
import com.api.loanflow.usuario.api.dto.ContaBancariaResponse;
import com.api.loanflow.usuario.api.dto.CredorLookupResponse;
import com.api.loanflow.usuario.api.dto.UsuarioResponse;
import com.api.loanflow.usuario.application.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping("/me")
	public UsuarioResponse me() {
		return usuarioService.meuPerfil();
	}

	@GetMapping("/me/conta-bancaria")
	public ContaBancariaResponse minhaContaBancaria() {
		return usuarioService.minhaContaBancaria();
	}

	@PutMapping("/me")
	public UsuarioResponse atualizarMeuPerfil(@Valid @RequestBody AtualizarPerfilRequest request) {
		return usuarioService.atualizarMeuPerfil(request);
	}

	@PutMapping("/me/conta-bancaria")
	public ContaBancariaResponse salvarMinhaContaBancaria(@Valid @RequestBody AtualizarContaBancariaRequest request) {
		return usuarioService.salvarMinhaContaBancaria(request);
	}

	@PutMapping("/me/senha")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void alterarMinhaSenha(@Valid @RequestBody AlterarSenhaRequest request) {
		usuarioService.alterarMinhaSenha(request);
	}

	@PutMapping("/me/dados-financeiros")
	public UsuarioResponse atualizarMeusDadosFinanceiros(@Valid @RequestBody AtualizarDadosFinanceirosRequest request) {
		return usuarioService.atualizarMeusDadosFinanceiros(request);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<UsuarioResponse> listar() {
		return usuarioService.listar();
	}

	@GetMapping("/credores")
	@PreAuthorize("isAuthenticated()")
	public List<CredorLookupResponse> listarCredores(@RequestParam(required = false) String nome) {
		return usuarioService.listarCredores(nome);
	}

	@PostMapping("/{id}/bloquear")
	@PreAuthorize("hasRole('ADMIN')")
	public UsuarioResponse bloquear(@PathVariable Long id, HttpServletRequest servletRequest) {
		return usuarioService.bloquear(id, servletRequest.getRemoteAddr());
	}

	@PostMapping("/{id}/reativar")
	@PreAuthorize("hasRole('ADMIN')")
	public UsuarioResponse reativar(@PathVariable Long id, HttpServletRequest servletRequest) {
		return usuarioService.reativar(id, servletRequest.getRemoteAddr());
	}
}
