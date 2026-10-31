package com.foodai.backend.security;

import com.foodai.backend.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private final JwtService jwtService = new JwtService(TEST_SECRET, 900_000);

    @Test
    void generatedTokenContainsOnlyIdentityAndRevocationClaims() {
        FoodAIUserDetails user = userDetails();

        var claims = jwtService.parseToken(jwtService.generateToken(user));

        assertEquals("42", claims.getSubject());
        assertEquals(7, ((Number) claims.get("ver")).intValue());
        assertEquals("foodai-api", claims.getIssuer());
        assertTrue(claims.getExpiration().after(new Date()));
        assertFalse(claims.containsKey("password"));
        assertFalse(claims.containsKey("email"));
        assertTrue(jwtService.isValidFor(claims, user));
    }

    @Test
    void rejectsExpiredToken() {
        Instant now = Instant.now();
        String expiredToken = Jwts.builder()
                .issuer("foodai-api")
                .subject("42")
                .claim("ver", 7)
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.parseToken(expiredToken));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String otherSecret = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
        String token = new JwtService(otherSecret, 900_000).generateToken(userDetails());

        assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> jwtService.parseToken(token));
    }

    @Test
    void passwordEncoderStoresOnlyBcryptHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "test-password-not-for-production";
        String encodedPassword = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2"));
        assertTrue(encoder.matches(rawPassword, encodedPassword));
    }

    private FoodAIUserDetails userDetails() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("user@example.test");
        when(user.getPasswordHash()).thenReturn("$2a$10$test-hash");
        when(user.getRole()).thenReturn(User.Role.USER);
        when(user.getAccountStatus()).thenReturn(User.AccountStatus.ACTIVE);
        when(user.getTokenVersion()).thenReturn(7);
        return FoodAIUserDetails.from(user);
    }
}
