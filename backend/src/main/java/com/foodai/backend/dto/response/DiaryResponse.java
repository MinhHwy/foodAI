package com.foodai.backend.dto.response;

import com.foodai.backend.entity.DiaryMeal;
import com.foodai.backend.entity.MealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class DiaryResponse {
    private DiaryResponse() {}

    public record Nutrition(BigDecimal caloriesKcal, BigDecimal proteinG,
                            BigDecimal carbsG, BigDecimal fatG) {}

    public record Item(Long id, Long foodId, Long recognitionItemId,
                       String foodName, String servingLabel, BigDecimal portionMultiplier,
                       BigDecimal consumedGrams, Nutrition nutrition) {}

    public record Meal(Long id, MealType mealType, LocalDateTime eatenAtUtc,
                       LocalDate localMealDate, DiaryMeal.SourceType sourceType,
                       String notes, Nutrition total, List<Item> items) {}

    public record History(LocalDate from, LocalDate to, Nutrition total,
                          List<Meal> meals) {}
}
