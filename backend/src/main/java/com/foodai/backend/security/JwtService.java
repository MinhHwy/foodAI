package com.foodai.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Service
public class JwtService {

    private static final String ISSUER = "foodai-api";
    private static final String TOKEN_VERSION_CLAIM = "ver";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${security.jwt.secret-base64}") String secretBase64,
            @Value("${security.jwt.expiration-ms}") long expirationMs) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be configured.");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT expiration must be greater than zero.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must contain valid Base64.", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must decode to at least 32 bytes.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(FoodAIUserDetails user) {
        Objects.requireNonNull(user, "Authenticated user is required.");
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getUserId().toString())
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        try {
            if (Long.parseLong(claims.getSubject()) <= 0) {
                throw new NumberFormatException("User ID must be positive.");
            }
        } catch (NumberFormatException exception) {
            throw new MalformedJwtException("JWT subject must be a positive user ID.", exception);
        }

        Object versionClaim = claims.get(TOKEN_VERSION_CLAIM);
        if (!(versionClaim instanceof Number version) || version.intValue() < 0) {
            throw new MalformedJwtException("JWT token version is missing or invalid.");
        }
        return claims;
    }

    public boolean isValidFor(Claims claims, FoodAIUserDetails user) {
        Object versionClaim = claims.get(TOKEN_VERSION_CLAIM);
        return user.getUserId().toString().equals(claims.getSubject())
                && versionClaim instanceof Number version
                && version.intValue() == user.getTokenVersion();
    }
}
