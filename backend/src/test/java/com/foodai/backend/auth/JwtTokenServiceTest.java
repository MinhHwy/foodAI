package com.foodai.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {
    private static final String SECRET = "foodai-jwt-test-secret-0123456789-abcdef";

    @Test
    void issuedTokenContainsStableUserIdentityAndVersion() {
        JwtTokenService service = new JwtTokenService(new JwtProperties(SECRET, 60));
        CurrentUser user = new CurrentUser(42L, "user@example.com", "Food User", "USER", 3);

        var claims = service.parse(service.issue(user));

        assertEquals("42", claims.getSubject());
        assertEquals("user@example.com", claims.get("email"));
        assertEquals("USER", claims.get("role"));
        assertEquals(3, ((Number) claims.get("ver")).intValue());
        assertEquals("user@example.com", user.getName());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtTokenService issuer = new JwtTokenService(new JwtProperties(SECRET, 60));
        JwtTokenService verifier = new JwtTokenService(
                new JwtProperties("another-jwt-test-secret-0123456789-abcdef", 60));

        assertNull(verifier.parse(issuer.issue(new CurrentUser(42L, "user@example.com", "Food User", "USER", 0))));
    }

    @Test
    void rejectsConfiguredSecretShorterThanHmacMinimum() {
        assertThrows(IllegalStateException.class,
                () -> new JwtTokenService(new JwtProperties("too-short", 60)));
    }
}
