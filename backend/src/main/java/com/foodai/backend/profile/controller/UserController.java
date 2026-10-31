package com.foodai.backend.profile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;
import com.foodai.backend.auth.CurrentUser;
import com.foodai.backend.profile.dto.GoalsUpdateRequest;
import com.foodai.backend.profile.dto.MeResponse;
import com.foodai.backend.profile.service.UserService;

/**
 * Every endpoint resolves the identity from the authenticated SecurityContext
 * (JWT filter). No user id is ever accepted from the client, so a user can
 * never read or write another user's profile through this controller.
 */
@RestController
@RequestMapping("/api/v1/me")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	MeResponse me(@AuthenticationPrincipal CurrentUser principal) {
		return userService.getMe(principal.userId());
	}

	@PatchMapping("/profile")
	MeResponse patchProfile(@AuthenticationPrincipal CurrentUser principal, @RequestBody JsonNode body) {
		return userService.patchProfile(principal.userId(), body);
	}

	@PatchMapping("/settings")
	MeResponse patchSettings(@AuthenticationPrincipal CurrentUser principal, @RequestBody JsonNode body) {
		return userService.patchSettings(principal.userId(), body);
	}

	@PutMapping("/goals")
	MeResponse putGoals(@AuthenticationPrincipal CurrentUser principal,
			@jakarta.validation.Valid @RequestBody GoalsUpdateRequest request) {
		return userService.putGoals(principal.userId(), request);
	}
}
