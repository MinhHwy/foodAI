package com.foodai.backend.profile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.foodai.backend.profile.entity.UserGoalEntity;
import com.foodai.backend.profile.entity.UserProfileEntity;
import com.foodai.backend.profile.entity.UserSettingsEntity;
import com.foodai.backend.profile.entity.UserEntity;

/**
 * Response of GET /api/v1/me and of every profile mutation. Sensitive fields
 * (password hash, token version, security internals) are intentionally absent.
 */
public record MeResponse(UserDto user, ProfileDto profile, SettingsDto settings, GoalDto goals) {

	public record UserDto(Long id, String fullName, String email, String role, LocalDateTime emailVerifiedAt,
			LocalDateTime createdAt) {

		public static UserDto from(UserEntity user) {
			return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
					user.getEmailVerifiedAt(), user.getCreatedAt());
		}
	}

	public record ProfileDto(Integer ageYears, String biologicalSex, BigDecimal heightCm, BigDecimal currentWeightKg,
			String activityLevel, String avatarUrl, LocalDateTime updatedAt) {

		public static ProfileDto from(UserProfileEntity profile) {
			return new ProfileDto(profile.getAgeYears(), profile.getBiologicalSex().name(), profile.getHeightCm(),
					profile.getCurrentWeightKg(), profile.getActivityLevel() == null ? null : profile.getActivityLevel().name(),
					profile.getAvatarUrl(), profile.getUpdatedAt());
		}
	}

	public record SettingsDto(String timezoneName, String energyUnit, String weightUnit, Boolean dailyRemindersEnabled,
			String dietaryNotes, LocalDateTime updatedAt) {

		public static SettingsDto from(UserSettingsEntity settings) {
			return new SettingsDto(settings.getTimezoneName(), settings.getEnergyUnit().name(),
					settings.getWeightUnit().name(), settings.getDailyRemindersEnabled(), settings.getDietaryNotes(),
					settings.getUpdatedAt());
		}
	}

	public record GoalDto(String goalType, BigDecimal targetWeightKg, BigDecimal dailyCaloriesKcal,
			BigDecimal proteinTargetG, BigDecimal carbsTargetG, BigDecimal fatTargetG, BigDecimal estimatedBmrKcal,
			BigDecimal estimatedTdeeKcal, String calculationMethod, LocalDate effectiveFrom, LocalDate effectiveTo) {

		public static GoalDto from(UserGoalEntity goal) {
			return new GoalDto(goal.getGoalType().name(), goal.getTargetWeightKg(), goal.getDailyCaloriesKcal(),
					goal.getProteinTargetG(), goal.getCarbsTargetG(), goal.getFatTargetG(), goal.getEstimatedBmrKcal(),
					goal.getEstimatedTdeeKcal(), goal.getCalculationMethod(), goal.getEffectiveFrom(),
					goal.getEffectiveTo());
		}
	}
}
