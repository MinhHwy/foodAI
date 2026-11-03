package com.foodai.backend.service;

import com.foodai.backend.dto.response.DiaryResponse;
import com.foodai.backend.entity.Food;
import com.foodai.backend.exception.FoodApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class NutritionCalculator {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public BigDecimal positive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new FoodApiException(HttpStatus.BAD_REQUEST, name + " must be greater than zero.");
        }
        return value;
    }

    public DiaryResponse.Nutrition forGrams(Food food, BigDecimal grams) {
        positive(grams, "consumedGrams");
        return new DiaryResponse.Nutrition(
                scale(food.getCaloriesPer100gKcal().multiply(grams).divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)),
                scale(food.getProteinPer100gG().multiply(grams).divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)),
                scale(food.getCarbsPer100gG().multiply(grams).divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)),
                scale(food.getFatPer100gG().multiply(grams).divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)));
    }

    public static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal multiplier(BigDecimal grams, Food food, BigDecimal fallback) {
        if (food.getStandardServingGrams() != null && food.getStandardServingGrams().signum() > 0) {
            BigDecimal ratio = grams.divide(food.getStandardServingGrams(), 3, RoundingMode.HALF_UP);
            return positive(ratio, "portionMultiplier");
        }
        return fallback == null ? BigDecimal.ONE.setScale(3) : positive(fallback, "portionMultiplier");
    }

    public BigDecimal grams(Food food, BigDecimal suppliedGrams, BigDecimal portions) {
        if (suppliedGrams != null) return positive(suppliedGrams, "consumedGrams");
        if (portions == null || portions.signum() <= 0 || food.getStandardServingGrams() == null) {
            throw new FoodApiException(HttpStatus.BAD_REQUEST,
                    "A positive consumedGrams is required when standard serving size is unavailable.");
        }
        BigDecimal result = food.getStandardServingGrams().multiply(portions).setScale(2, RoundingMode.HALF_UP);
        return positive(result, "consumedGrams");
    }

    public static DiaryResponse.Nutrition zero() {
        return new DiaryResponse.Nutrition(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2));
    }

    public static DiaryResponse.Nutrition plus(DiaryResponse.Nutrition a, DiaryResponse.Nutrition b) {
        return new DiaryResponse.Nutrition(
                a.caloriesKcal().add(b.caloriesKcal()), a.proteinG().add(b.proteinG()),
                a.carbsG().add(b.carbsG()), a.fatG().add(b.fatG()));
    }
}
