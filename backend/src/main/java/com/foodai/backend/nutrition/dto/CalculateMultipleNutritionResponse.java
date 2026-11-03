package com.foodai.backend.nutrition.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response of POST /api/v1/nutrition/calculate-multiple. `items` preserves the
 * request order and contains each portion calculated independently; `totals`
 * is the sum of the per-item values (therefore equals the sum of the item
 * results exactly, with no re-rounding drift).
 */
public record CalculateMultipleNutritionResponse(int itemCount, List<NutritionCalculationResult> items,
		NutritionTotals totals) {

	/** Sum over all portions. weight in g, calories in kcal, macros in g. */
	public record NutritionTotals(BigDecimal weight, BigDecimal caloriesKcal, BigDecimal proteinG, BigDecimal carbsG,
			BigDecimal fatG) {
	}
}
