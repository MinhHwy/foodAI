package com.foodai.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration. Values come from application.properties with env-variable override
 * (FOODAI_JWT_SECRET). The secret must never be committed to source control.
 */
@ConfigurationProperties(prefix = "foodai.jwt")
public record JwtProperties(String secret, int validityMinutes) {
}
