package com.foodai.backend.food.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.foodai.backend.common.error.ApiException;
import com.foodai.backend.food.dto.FoodNutritionResponse;
import com.foodai.backend.food.entity.FoodEntity;
import com.foodai.backend.food.repository.FoodRepository;

/**
 * GET /api/v1/foods/{foodId}/nutrition — publishes the raw nutrition data of a
 * food exactly as stored in the `foods` table (per-100g reference values plus
 * the standard serving portion). This endpoint responds with data only; the
 * scaled calculations for requested weights live in the nutrition module
 * (POST /api/v1/nutrition/calculate…).
 */
@RestController
@RequestMapping("/api/v1/foods")
public class FoodController {

	private final FoodRepository foodRepository;

	public FoodController(FoodRepository foodRepository) {
		this.foodRepository = foodRepository;
	}

	@GetMapping("/{foodId}/nutrition")
	FoodNutritionResponse nutrition(@PathVariable Long foodId) {
		FoodEntity food = foodRepository.findById(foodId)
				.orElseThrow(() -> new ApiException.NotFoundException("Món ăn không tồn tại (id=" + foodId + ")."));
		return FoodNutritionResponse.from(food);
	}
}
