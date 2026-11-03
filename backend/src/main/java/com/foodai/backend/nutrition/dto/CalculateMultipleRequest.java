package com.foodai.backend.nutrition.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/nutrition/calculate-multiple. The list may not be empty
 * and every item is validated; order of items is preserved in the response.
 */
public record CalculateMultipleRequest(
		@NotEmpty(message = "items không được để trống; cần ít nhất một món ăn.")
		@Size(max = 100, message = "Tối đa 100 món ăn mỗi yêu cầu.")
		List<@Valid NutritionCalculationItem> items) {
}
