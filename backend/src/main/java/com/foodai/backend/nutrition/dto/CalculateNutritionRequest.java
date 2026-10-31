package com.foodai.backend.nutrition.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body of POST /api/v1/nutrition/calculate for one food:
 * {"foodId": 1, "weight": 200}. `foodId` references `foods.id`; `weight` is
 * the requested portion in GRAMS.
 */
public record CalculateNutritionRequest(
		@NotNull(message = "foodId là bắt buộc.")
		@Positive(message = "foodId phải là số nguyên dương.")
		Long foodId,

		@NotNull(message = "Trọng lượng (gram) là bắt buộc.")
		@Positive(message = "Trọng lượng (gram) phải lớn hơn 0.")
		@Digits(integer = 7, fraction = 2, message = "Trọng lượng (gram) tối đa là 9.999.999,99 g và tối đa 2 chữ số thập phân.")
		BigDecimal weight) {
}
