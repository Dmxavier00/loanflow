package com.api.loanflow.compartilhado.validacao;

import com.api.loanflow.compartilhado.excecao.RegraNegocioException;

public final class DocumentoValidator {
	private DocumentoValidator() {
	}

	public static String normalizarCpf(String cpf) {
		var digits = onlyDigits(cpf);
		if (digits.length() != 11) {
			throw new RegraNegocioException("CPF deve conter 11 dígitos.");
		}
		if (!isCpfValido(digits)) {
			throw new RegraNegocioException("CPF inválido.");
		}
		return digits;
	}

	public static String normalizarCep(String cep) {
		var digits = onlyDigits(cep);
		if (digits.length() != 8) {
			throw new RegraNegocioException("CEP deve conter 8 dígitos.");
		}
		return formatCep(digits);
	}

	public static boolean isCpfValido(String cpf) {
		var digits = onlyDigits(cpf);
		if (digits.length() != 11 || allDigitsEqual(digits)) {
			return false;
		}

		var firstDigit = calculateCpfDigit(digits.substring(0, 9), 10);
		var secondDigit = calculateCpfDigit(digits.substring(0, 9) + firstDigit, 11);
		return digits.equals(digits.substring(0, 9) + firstDigit + secondDigit);
	}

	public static boolean isCepValido(String cep) {
		return onlyDigits(cep).length() == 8;
	}

	private static String onlyDigits(String value) {
		return value == null ? "" : value.replaceAll("\\D", "");
	}

	private static boolean allDigitsEqual(String value) {
		var first = value.charAt(0);
		for (int index = 1; index < value.length(); index++) {
			if (value.charAt(index) != first) {
				return false;
			}
		}
		return true;
	}

	private static int calculateCpfDigit(String base, int weightStart) {
		var total = 0;
		for (int index = 0; index < base.length(); index++) {
			total += Character.getNumericValue(base.charAt(index)) * (weightStart - index);
		}

		var remainder = total % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}

	private static String formatCep(String digits) {
		return digits.substring(0, 5) + "-" + digits.substring(5);
	}
}
