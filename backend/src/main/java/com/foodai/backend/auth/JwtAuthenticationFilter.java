package com.foodai.backend.auth;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.foodai.backend.profile.entity.UserEntity;
import com.foodai.backend.profile.repository.UserRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Validates the "Bearer" token of every request, loads the matching user from
 * the database (checking token_version and account status) and populates the
 * SecurityContext. Invalid, expired or stale tokens simply leave the request
 * anonymous; protected endpoints then answer 401 through the entry point.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenService tokenService;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtTokenService tokenService, UserRepository userRepository) {
		this.tokenService = tokenService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
			Claims claims = tokenService.parse(header.substring("Bearer ".length()).trim());
			if (claims != null) {
				resolveAuthentication(request, claims);
			}
		}
		chain.doFilter(request, response);
	}

	private void resolveAuthentication(HttpServletRequest request, Claims claims) {
		Long userId = parseSubject(claims.getSubject());
		if (userId == null) {
			return;
		}
		Long claimVersion = extractVersion(claims.get("ver"));
		UserEntity user = userRepository.findById(userId).orElse(null);
		if (user == null || claimVersion == null || claimVersion != user.getTokenVersion()
				|| user.getAccountStatus() != UserEntity.AccountStatus.ACTIVE) {
			return;
		}
		CurrentUser principal = new CurrentUser(user.getId(), user.getEmail(), user.getFullName(),
				user.getRole().name(), user.getTokenVersion());
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null,
				principal.authorities());
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private static Long parseSubject(String subject) {
		try {
			return Long.valueOf(subject);
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	private static Long extractVersion(Object verClaim) {
		if (verClaim instanceof Number number) {
			return number.longValue();
		}
		if (verClaim instanceof String text) {
			try {
				return Long.valueOf(text);
			}
			catch (NumberFormatException ex) {
				return null;
			}
		}
		return null;
	}
}
