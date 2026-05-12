package com.api.loanflow.shared.cep.api;

import com.api.loanflow.shared.cep.api.dto.CepLookupResponse;
import com.api.loanflow.shared.cep.application.CepLookupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enderecos")
public class CepLookupController {
	private final CepLookupService cepLookupService;

	public CepLookupController(CepLookupService cepLookupService) {
		this.cepLookupService = cepLookupService;
	}

	@GetMapping("/cep/{cep}")
	public CepLookupResponse buscarPorCep(@PathVariable String cep) {
		return cepLookupService.buscarPorCep(cep);
	}
}
