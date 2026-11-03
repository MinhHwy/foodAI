package com.foodai.backend;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Shared helpers for the integration tests. The JWT secret used here is
 * injected into the application under test through Spring property overrides,
 * never into application.properties.
 */
public final class TestSupport {

	public static final String JWT_SECRET = "foodai-local-test-secret-key-32-bytes!!!";
	public static final Duration TOKEN_VALIDITY = Duration.ofMinutes(1440);

	private TestSupport() {
	}

	public static String uniqueEmail() {
		return ("tester-" + UUID.randomUUID() + "@example.com");
	}

	public static String validToken(Long userId, String email, int tokenVersion) {
		return token(userId, email, tokenVersion, Instant.now(), Instant.now().plus(TOKEN_VALIDITY));
	}

	public static String expiredToken(Long userId, String email, int tokenVersion) {
		return token(userId, email, tokenVersion, Instant.now().minus(Duration.ofHours(2)),
				Instant.now().minus(Duration.ofHours(1)));
	}

	private static String token(Long userId, String email, int tokenVersion, Instant issuedAt, Instant expiration) {
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("email", email)
				.claim("role", "USER")
				.claim("ver", tokenVersion)
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiration))
				.signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
				.compact();
	}
}
