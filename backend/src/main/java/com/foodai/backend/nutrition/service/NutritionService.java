package com.foodai.backend.nutrition.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodai.backend.common.error.ApiException.NotFoundException;
import com.foodai.backend.dto.response.DiaryResponse;
import com.foodai.backend.entity.Food;
import com.foodai.backend.repository.FoodRepository;
import com.foodai.backend.service.NutritionCalculator;
import com.foodai.backend.nutrition.dto.CalculateMultipleNutritionResponse;
import com.foodai.backend.nutrition.dto.CalculateMultipleNutritionResponse.NutritionTotals;
import com.foodai.backend.nutrition.dto.NutritionCalculationItem;
import com.foodai.backend.nutrition.dto.NutritionCalculationResult;

/**
 * Nutrition calculation service. `foods` stores every nutritional value per
 * 100 g (calories kcal, protein/carbs/fat g), so:
 *
 *   calculatedValue = referenceValue * requestedWeight / 100
 *
 * Rounding policy (project-wide, matches diary_items decimal(10,2) and the
 * profile goal calculations): all results use BigDecimal with scale 2 and
 * RoundingMode.HALF_UP, applied at the end of each computation only.
 *
 * The nutritional columns in the database are NOT NULL, so nutrition data can
 * never be missing and no value is silently zero-filled. Only
 * standard_serving_grams may be null; that transparency is preserved in the
 * API responses instead.
 */
@Service
public class NutritionService {

	private final FoodRepository foodRepository;
	private final NutritionCalculator calculator;

	public NutritionService(FoodRepository foodRepository, NutritionCalculator calculator) {
		this.foodRepository = foodRepository;
		this.calculator = calculator;
	}

	@Transactional(readOnly = true)
	public NutritionCalculationResult calculate(Long foodId, BigDecimal weight) {
		Food food = loadFood(foodId);
		return toResult(food, weight);
	}

	/**
	 * Calculates each item independently and returns the results in the exact
	 * request order. Foods are loaded once with a single query (duplicate
	 * foodIds never trigger extra fetches or double lookups). If any food does
	 * not exist the whole request fails with 404 listing every missing id.
	 */
	@Transactional(readOnly = true)
	public CalculateMultipleNutritionResponse calculateMultiple(List<NutritionCalculationItem> items) {
		List<Long> foodIds = items.stream().map(NutritionCalculationItem::foodId).toList();
		Map<Long, Food> foodById = foodRepository.findAllById(foodIds).stream()
				.collect(Collectors.toMap(Food::getId, Function.identity(), (first, second) -> first));
		List<Long> missingIds = foodIds.stream().distinct().filter(id -> !foodById.containsKey(id)).toList();
		if (!missingIds.isEmpty()) {
			throw new NotFoundException(
					"Món ăn không tồn tại (id=" + missingIds.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ").");
		}

		List<NutritionCalculationResult> results = items.stream()
				.map(item -> toResult(foodById.get(item.foodId()), item.weight()))
				.toList();

		BigDecimal totalWeight = results.stream().map(NutritionCalculationResult::weight)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCalories = sum(results.stream().map(NutritionCalculationResult::caloriesKcal));
		BigDecimal totalProtein = sum(results.stream().map(NutritionCalculationResult::proteinG));
		BigDecimal totalCarbs = sum(results.stream().map(NutritionCalculationResult::carbsG));
		BigDecimal totalFat = sum(results.stream().map(NutritionCalculationResult::fatG));

		return new CalculateMultipleNutritionResponse(results.size(), results,
				new NutritionTotals(totalWeight, totalCalories, totalProtein, totalCarbs, totalFat));
	}

	private NutritionCalculationResult toResult(Food food, BigDecimal weight) {
		DiaryResponse.Nutrition nutrition = calculator.forGrams(food, weight);
		return new NutritionCalculationResult(food.getId(), food.getNameVi(), weight,
				nutrition.caloriesKcal(), nutrition.proteinG(), nutrition.carbsG(), nutrition.fatG());
	}

	private Food loadFood(Long foodId) {
		return foodRepository.findById(foodId)
				.orElseThrow(() -> new NotFoundException("Món ăn không tồn tại (id=" + foodId + ")."));
	}

	private BigDecimal sum(java.util.stream.Stream<BigDecimal> values) {
		return values.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}
