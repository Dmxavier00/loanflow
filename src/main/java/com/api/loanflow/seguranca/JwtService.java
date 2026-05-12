package com.api.loanflow.seguranca;

import com.api.loanflow.usuario.dominio.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
	private final SecretKey signingKey;
	private final long expirationSeconds;

	public JwtService(
		@Value("${loanflow.jwt.secret}") String secret,
		@Value("${loanflow.jwt.expiration-minutes}") long expirationMinutes
	) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT secret nao configurado. Defina loanflow.jwt.secret ou JWT_SECRET.");
		}

		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalStateException("JWT secret deve conter ao menos 32 bytes.");
		}

		this.signingKey = Keys.hmacShaKeyFor(secretBytes);
		this.expirationSeconds = expirationMinutes * 60;
	}

	public String gerarToken(String email, Role papel) {
		var now = Instant.now();
		return Jwts.builder()
			.subject(email)
			.claim("role", papel == null ? null : papel.name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(expirationSeconds)))
			.signWith(signingKey)
			.compact();
	}

	public String extrairEmail(String token) {
		return Jwts.parser()
			.verifyWith(signingKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getSubject();
	}

	public long getExpirationSeconds() {
		return expirationSeconds;
	}
}
