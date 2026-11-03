package com.foodai.backend.service;

import com.foodai.backend.dto.request.CreateDiaryMealRequest;
import com.foodai.backend.dto.response.DiaryResponse;
import com.foodai.backend.entity.DiaryItem;
import com.foodai.backend.entity.DiaryMeal;
import com.foodai.backend.entity.Food;
import com.foodai.backend.entity.MealType;
import com.foodai.backend.entity.User;
import com.foodai.backend.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FoodDiaryServiceTest {
    @Test
    void rejectsEmptyMealPatchBeforeDatabaseAccess() {
        DiaryMealRepository meals = mock(DiaryMealRepository.class);
        FoodDiaryService service = new FoodDiaryService(meals,
                mock(DiaryItemRepository.class), mock(FoodRepository.class),
                mock(RecognitionItemRepository.class), mock(UserSettingsRepository.class),
                new NutritionCalculator());
        assertThrows(com.foodai.backend.exception.FoodApiException.class,
                () -> service.updateMeal(new User(7L), 99L,
                        new com.foodai.backend.dto.request.UpdateDiaryMealRequest(null, null, null)));
        verifyNoInteractions(meals);
    }

    @Test
    void rejectsEmptyItemUpdate() {
        DiaryMealRepository meals = mock(DiaryMealRepository.class);
        DiaryItemRepository items = mock(DiaryItemRepository.class);
        User user = new User(7L);
        DiaryMeal meal = new DiaryMeal();
        meal.setUser(user);
        when(meals.findByIdAndUserId(99L, 7L)).thenReturn(Optional.of(meal));
        FoodDiaryService service = new FoodDiaryService(meals, items, mock(FoodRepository.class),
                mock(RecognitionItemRepository.class), mock(UserSettingsRepository.class),
                new NutritionCalculator());
        assertThrows(com.foodai.backend.exception.FoodApiException.class,
                () -> service.updateItem(user, 99L, 101L,
                        new com.foodai.backend.dto.request.UpdateDiaryItemRequest(null, null, null)));
        verifyNoInteractions(items);
    }

    @Test
    void createsCorrectNutritionSnapshotAndLocalDate() {
        DiaryMealRepository meals = mock(DiaryMealRepository.class);
        DiaryItemRepository items = mock(DiaryItemRepository.class);
        FoodRepository foods = mock(FoodRepository.class);
        RecognitionItemRepository recognitionItems = mock(RecognitionItemRepository.class);
        UserSettingsRepository settings = mock(UserSettingsRepository.class);
        FoodDiaryService service = new FoodDiaryService(meals, items, foods,
                recognitionItems, settings, new NutritionCalculator());

        Food food = mock(Food.class);
        when(food.getId()).thenReturn(1L);
        when(food.getNameVi()).thenReturn("Pho ga");
        when(food.getCatalogStatus()).thenReturn(Food.CatalogStatus.PUBLISHED);
        when(food.getStandardServingLabel()).thenReturn("1 bowl");
        when(food.getStandardServingGrams()).thenReturn(new BigDecimal("500.00"));
        when(food.getCaloriesPer100gKcal()).thenReturn(new BigDecimal("84.00"));
        when(food.getProteinPer100gG()).thenReturn(new BigDecimal("7.60"));
        when(food.getCarbsPer100gG()).thenReturn(new BigDecimal("10.40"));
        when(food.getFatPer100gG()).thenReturn(new BigDecimal("2.40"));
        when(foods.findById(1L)).thenReturn(Optional.of(food));

        when(meals.save(any(DiaryMeal.class))).thenAnswer(inv -> {
            DiaryMeal meal = inv.getArgument(0);
            ReflectionTestUtils.setField(meal, "id", 99L);
            return meal;
        });
        AtomicReference<DiaryItem> created = new AtomicReference<>();
        when(items.save(any(DiaryItem.class))).thenAnswer(inv -> {
            DiaryItem item = inv.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 100L);
            created.set(item);
            return item;
        });
        when(items.findByMealIdOrderByIdAsc(99L)).thenAnswer(inv -> List.of(created.get()));

        CreateDiaryMealRequest request = new CreateDiaryMealRequest(MealType.LUNCH,
                OffsetDateTime.parse("2026-11-01T12:30:00+07:00"), "Lunch",
                List.of(new CreateDiaryMealRequest.Item(1L, null, null, new BigDecimal("150.00"))));
        DiaryResponse.Meal result = service.create(new User(7L), request);

        assertEquals(LocalDate.of(2026, 11, 1), result.localMealDate());
        assertEquals(DiaryMeal.SourceType.MANUAL, result.sourceType());
        assertEquals(new BigDecimal("126.00"), result.total().caloriesKcal());
        assertEquals(new BigDecimal("0.300"), created.get().getPortionMultiplier());
        assertEquals("Pho ga", created.get().getFoodNameSnapshot());
    }
}
