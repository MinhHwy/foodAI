package com.foodai.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtTokenService {

	private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
	private static final int MIN_SECRET_BYTES = 32;

	private final SecretKey signingKey;
	private final Duration validity;

	@Autowired
	public JwtTokenService(JwtProperties properties) {
		this.validity = Duration.ofMinutes(properties.validityMinutes() > 0 ? properties.validityMinutes() : 1440);
		String secret = properties.secret();
		if (secret == null || secret.isBlank()) {
			byte[] ephemeral = new byte[64];
			new SecureRandom().nextBytes(ephemeral);
			this.signingKey = Keys.hmacShaKeyFor(ephemeral);
			log.warn("FOODAI_JWT_SECRET is not configured - an ephemeral random signing key was generated. "
					+ "All issued tokens become invalid on restart. Set FOODAI_JWT_SECRET for production use.");
		}
		else {
			byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
			if (secretBytes.length < MIN_SECRET_BYTES) {
				throw new IllegalStateException(
						"foodai.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (32 characters).");
			}
			this.signingKey = Keys.hmacShaKeyFor(secretBytes);
		}
	}

	public String issue(CurrentUser principal) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(principal.userId()))
				.claim("email", principal.email())
				.claim("role", principal.role())
				.claim("ver", principal.tokenVersion())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(validity)))
				.signWith(signingKey)
				.compact();
	}

	/**
	 * @return parsed claims for a valid, non-expired token; null if the token is
	 *         malformed, has an invalid signature or has expired.
	 */
	public Claims parse(String token) {
		try {
			return Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		}
		catch (JwtException | IllegalArgumentException ex) {
			return null;
		}
	}

	public long expiresInSeconds() {
		return validity.toSeconds();
	}
}
