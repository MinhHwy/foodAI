package com.foodai.backend.auth;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodai.backend.auth.dto.AuthResponse;
import com.foodai.backend.auth.dto.LoginRequest;
import com.foodai.backend.auth.dto.RegisterRequest;
import com.foodai.backend.auth.dto.UserSummaryResponse;
import com.foodai.backend.common.error.ApiException.ConflictException;
import com.foodai.backend.common.error.ApiException.ForbiddenException;
import com.foodai.backend.common.error.ApiException.UnauthorizedException;
import com.foodai.backend.common.time.UtcTimes;
import com.foodai.backend.entity.User;
import com.foodai.backend.entity.UserProfile;
import com.foodai.backend.entity.UserSettings;
import com.foodai.backend.repository.UserProfileRepository;
import com.foodai.backend.repository.UserRepository;
import com.foodai.backend.repository.UserSettingsRepository;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;
	private final UserSettingsRepository userSettingsRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;

	public AuthService(UserRepository userRepository, UserProfileRepository userProfileRepository,
			UserSettingsRepository userSettingsRepository, PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService) {
		this.userRepository = userRepository;
		this.userProfileRepository = userProfileRepository;
		this.userSettingsRepository = userSettingsRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
	}

	@Transactional
	public UserSummaryResponse register(RegisterRequest request) {
		// Canonical email normalization: one written convention for uniqueness checks
		// and storage, so duplicated registration via case variants cannot happen.
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String fullName = request.fullName().trim();
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("Email đã được sử dụng. Vui lòng dùng email khác.");
		}
		User user = new User();
		user.setFullName(fullName);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(User.Role.USER);
		user.setAccountStatus(User.AccountStatus.ACTIVE);
		// The current product flow verifies email via the frontend wizard; email gets
		// marked verified at registration so the account is immediately usable. When a
		// mail-delivery service is introduced, this should move to the OTP flow.
		user.setEmailVerifiedAt(UtcTimes.nowUtc());
		user = userRepository.save(user);

		UserProfile profile = new UserProfile();
		profile.setUser(user);
		profile.setBiologicalSex(UserProfile.BiologicalSex.UNSPECIFIED);
		userProfileRepository.save(profile);

		UserSettings settings = new UserSettings();
		settings.setUser(user);
		settings.setTimezoneName(UtcTimes.DEFAULT_ZONE.getId());
		settings.setEnergyUnit(UserSettings.EnergyUnit.KCAL);
		settings.setWeightUnit(UserSettings.WeightUnit.KG);
		settings.setDailyRemindersEnabled(Boolean.TRUE);
		userSettingsRepository.save(settings);

		log.info("New user registered: id={}", user.getId());
		return new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail());
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
		if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new UnauthorizedException("Email hoặc mật khẩu không chính xác.");
		}
		if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
			throw new ForbiddenException("Tài khoản hiện không thể đăng nhập. Vui lòng liên hệ bộ phận hỗ trợ.");
		}
		CurrentUser principal = new CurrentUser(user.getId(), user.getEmail(), user.getFullName(),
				user.getRole().name(), user.getTokenVersion());
		String token = jwtTokenService.issue(principal);
		return AuthResponse.bearer(token, jwtTokenService.expiresInSeconds(),
				new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail()));
	}
}
