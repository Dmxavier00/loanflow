package com.api.loanflow.compartilhado.criptografia;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class HashService {
	public String sha256(String value) {
		return sha256(value.getBytes(StandardCharsets.UTF_8));
	}

	public String sha256(byte[] value) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			var hash = digest.digest(value);
			return toHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Algoritmo SHA-256 indisponivel.", exception);
		}
	}

	private String toHex(byte[] bytes) {
		var builder = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			builder.append(String.format("%02x", value));
		}
		return builder.toString();
	}
}
