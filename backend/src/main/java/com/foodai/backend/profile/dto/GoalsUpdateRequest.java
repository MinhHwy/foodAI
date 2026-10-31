package com.foodai.backend.profile.dto;

import java.math.BigDecimal;

import com.foodai.backend.profile.entity.UserGoalEntity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Body of PUT /api/v1/me/goals. The backend computes BMR (Mifflin-St Jeor),
 * TDEE (activity multiplier), daily calories (goal policy) and macro targets
 * (30/45/25 split). The client only selects the goal type and optionally a
 * target weight.
 */
public record GoalsUpdateRequest(
		@NotNull(message = "Goal type is required; allowed values: LOSE, MAINTAIN, GAIN")
		UserGoalEntity.GoalType goalType,

		@DecimalMin(value = "0.01", message = "Target weight must be greater than 0")
		@DecimalMax(value = "600.00", message = "Target weight must be at most 600 kg")
		BigDecimal targetWeightKg) {
}
