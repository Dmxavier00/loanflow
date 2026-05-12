package com.api.loanflow.compartilhado.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
	@GetMapping(
		value = {
		"/",
		"/login",
		"/cadastro",
		"/dashboard",
		"/painel-de-controle",
		"/minha-conta",
		"/solicitacoes",
		"/propostas",
		"/propostas-credor",
		"/contratos",
		"/parcelas",
		"/alertas",
		"/central-notificacoes"
	},
		headers = "Sec-Fetch-Mode=navigate"
	)
	public String forwardToIndex() {
		return "forward:/index.html";
	}
}
