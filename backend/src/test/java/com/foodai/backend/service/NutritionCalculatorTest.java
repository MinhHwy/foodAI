package com.foodai.backend.service;

import com.foodai.backend.dto.response.DiaryResponse;
import com.foodai.backend.entity.Food;
import com.foodai.backend.exception.FoodApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NutritionCalculatorTest {
    private final NutritionCalculator calculator = new NutritionCalculator();

    @Test
    void calculatesNutritionFor150GramsUsingPer100gValues() {
        Food food = mock(Food.class);
        when(food.getCaloriesPer100gKcal()).thenReturn(new BigDecimal("84.00"));
        when(food.getProteinPer100gG()).thenReturn(new BigDecimal("7.60"));
        when(food.getCarbsPer100gG()).thenReturn(new BigDecimal("10.40"));
        when(food.getFatPer100gG()).thenReturn(new BigDecimal("2.40"));
        DiaryResponse.Nutrition result = calculator.forGrams(food, new BigDecimal("150.00"));
        assertEquals(new BigDecimal("126.00"), result.caloriesKcal());
        assertEquals(new BigDecimal("11.40"), result.proteinG());
        assertEquals(new BigDecimal("15.60"), result.carbsG());
        assertEquals(new BigDecimal("3.60"), result.fatG());
    }

    @Test
    void rejectsZeroOrMissingPortion() {
        Food food = mock(Food.class);
        assertThrows(FoodApiException.class, () -> calculator.forGrams(food, BigDecimal.ZERO));
        assertThrows(FoodApiException.class, () -> calculator.grams(food, null, null));
    }
}
