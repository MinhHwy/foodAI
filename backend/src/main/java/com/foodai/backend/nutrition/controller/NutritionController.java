package com.foodai.backend.nutrition.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.foodai.backend.nutrition.dto.CalculateMultipleNutritionResponse;
import com.foodai.backend.nutrition.dto.CalculateMultipleRequest;
import com.foodai.backend.nutrition.dto.CalculateNutritionRequest;
import com.foodai.backend.nutrition.dto.NutritionCalculationResult;
import com.foodai.backend.nutrition.service.NutritionService;

import jakarta.validation.Valid;

/**
 * Nutrition calculation APIs. Weight is always in grams; results are computed
 * from the per-100g values stored in the `foods` table (see NutritionService).
 */
@RestController
@RequestMapping("/api/v1/nutrition")
public class NutritionController {

	private final NutritionService nutritionService;

	public NutritionController(NutritionService nutritionService) {
		this.nutritionService = nutritionService;
	}

	@PostMapping("/calculate")
	NutritionCalculationResult calculate(@Valid @RequestBody CalculateNutritionRequest request) {
		return nutritionService.calculate(request.foodId(), request.weight());
	}

	@PostMapping("/calculate-multiple")
	CalculateMultipleNutritionResponse calculateMultiple(@Valid @RequestBody CalculateMultipleRequest request) {
		return nutritionService.calculateMultiple(request.items());
	}
}
