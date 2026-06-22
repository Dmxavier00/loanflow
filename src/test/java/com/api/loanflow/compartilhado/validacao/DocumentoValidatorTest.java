package com.api.loanflow.compartilhado.validacao;

import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoValidatorTest {
	@Test
	void aceitaCpfValidoComMascara() {
		assertEquals("52998224725", DocumentoValidator.normalizarCpf("529.982.247-25"));
	}

	@Test
	void rejeitaCpfComDigitosVerificadoresInvalidos() {
		assertFalse(DocumentoValidator.isCpfValido("529.982.247-24"));
		assertThrows(RegraNegocioException.class, () -> DocumentoValidator.normalizarCpf("529.982.247-24"));
	}

	@Test
	void rejeitaCpfComTodosOsDigitosIguais() {
		assertFalse(DocumentoValidator.isCpfValido("111.111.111-11"));
	}

	@Test
	void aceitaCepValidoComOuSemMascara() {
		assertTrue(DocumentoValidator.isCepValido("01001000"));
		assertTrue(DocumentoValidator.isCepValido("01001-000"));
		assertEquals("01001-000", DocumentoValidator.normalizarCep("01001000"));
	}

	@Test
	void rejeitaCepComQuantidadeInvalidaDeDigitos() {
		assertFalse(DocumentoValidator.isCepValido("12345-67"));
		assertThrows(RegraNegocioException.class, () -> DocumentoValidator.normalizarCep("12345-67"));
	}
}
