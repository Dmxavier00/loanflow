package com.api.loanflow.shared.crypto;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class HashService {
	public String sha256(String value) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			var hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return toHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Algoritmo SHA-256 indisponível.", exception);
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
