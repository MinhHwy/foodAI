package com.foodai.backend.auth;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Identity of the authenticated user, resolved from the JWT and the database.
 * It is attached to the SecurityContext by the JWT filter and consumed with
 * {@code @AuthenticationPrincipal}.
 */
public record CurrentUser(Long userId, String email, String fullName, String role, int tokenVersion)
		implements AuthenticatedPrincipal {

	@Override
	public String getName() {
		return email;
	}

	public List<GrantedAuthority> authorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}
}
