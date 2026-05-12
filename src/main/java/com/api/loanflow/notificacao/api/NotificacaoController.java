package com.api.loanflow.notificacao.api;

import com.api.loanflow.notificacao.api.dto.NotificacaoResponse;
import com.api.loanflow.notificacao.aplicacao.NotificacaoService;
import com.api.loanflow.notificacao.dominio.TipoNotificacao;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {
	private final NotificacaoService notificacaoService;

	public NotificacaoController(NotificacaoService notificacaoService) {
		this.notificacaoService = notificacaoService;
	}

	@GetMapping
	public List<NotificacaoResponse> minhas(
		@RequestParam(required = false) Boolean lida,
		@RequestParam(required = false) TipoNotificacao tipo
	) {
		return notificacaoService.listarMinhas(lida, tipo);
	}

	@PostMapping("/{id}/lida")
	public NotificacaoResponse marcarComoLida(@PathVariable Long id) {
		return notificacaoService.marcarComoLida(id);
	}
}
