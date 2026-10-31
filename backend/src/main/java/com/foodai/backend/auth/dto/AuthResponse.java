package com.foodai.backend.auth.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserSummaryResponse user) {

	public static AuthResponse bearer(String token, long expiresInSeconds, UserSummaryResponse user) {
		return new AuthResponse(token, "Bearer", expiresInSeconds, user);
	}
}
