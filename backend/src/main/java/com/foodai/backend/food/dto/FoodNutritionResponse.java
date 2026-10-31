package com.foodai.backend.food.dto;

import java.math.BigDecimal;

import com.foodai.backend.food.entity.FoodEntity;

/**
 * Response of GET /api/v1/foods/{foodId}/nutrition. The nutritional values are
 * always published per 100 g (see nutritionReference) with the unit encoded in
 * each field name (kcal / g), mirroring the flat DTO style of the profile
 * module. standardServingGrams is the reference portion and may be null when
 * the catalog does not know it (never silently replaced by 0).
 */
public record FoodNutritionResponse(Long foodId, String nameVi, String nameEn, String nutritionReference,
		String standardServingLabel, BigDecimal standardServingGrams, BigDecimal caloriesPer100gKcal,
		BigDecimal proteinPer100gG, BigDecimal carbsPer100gG, BigDecimal fatPer100gG) {

	public static FoodNutritionResponse from(FoodEntity food) {
		return new FoodNutritionResponse(food.getId(), food.getNameVi(), food.getNameEn(),
				FoodNutritionReference.PER_100G, food.getStandardServingLabel(), food.getStandardServingGrams(),
				food.getCaloriesPer100gKcal(), food.getProteinPer100gG(), food.getCarbsPer100gG(),
				food.getFatPer100gG());
	}

	/**
	 * The single reference scale stored in the `foods` table. Nutritional
	 * columns (…_per_100g_…) are normalized per 100 g, so the calculation
	 * formula is value * weight / 100.
	 */
	public static final class FoodNutritionReference {
		public static final String PER_100G = "per_100g";

		private FoodNutritionReference() {
		}
	}
}
