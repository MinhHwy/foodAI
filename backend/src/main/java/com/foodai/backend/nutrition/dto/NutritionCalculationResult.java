package com.foodai.backend.nutrition.dto;

import java.math.BigDecimal;

/**
 * Calculated nutrition for one requested portion of a food. All values are
 * rounded to 2 decimal places (HALF_UP) — the project-wide policy, matching
 * the decimal(10,2) columns of diary_items. Units: kcal for calories, g for
 * protein/carbs/fat/weight.
 */
public record NutritionCalculationResult(Long foodId, String foodName, BigDecimal weight, BigDecimal caloriesKcal,
		BigDecimal proteinG, BigDecimal carbsG, BigDecimal fatG) {
}
