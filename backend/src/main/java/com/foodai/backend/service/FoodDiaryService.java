package com.foodai.backend.service;

import com.foodai.backend.dto.request.CreateDiaryMealRequest;
import com.foodai.backend.dto.request.UpdateDiaryItemRequest;
import com.foodai.backend.dto.request.UpdateDiaryMealRequest;
import com.foodai.backend.dto.response.DiaryResponse;
import com.foodai.backend.entity.DiaryItem;
import com.foodai.backend.entity.DiaryMeal;
import com.foodai.backend.entity.Food;
import com.foodai.backend.entity.RecognitionItem;
import com.foodai.backend.entity.RecognitionSession;
import com.foodai.backend.entity.User;
import com.foodai.backend.exception.FoodApiException;
import com.foodai.backend.repository.DiaryItemRepository;
import com.foodai.backend.repository.DiaryMealRepository;
import com.foodai.backend.repository.FoodRepository;
import com.foodai.backend.repository.RecognitionItemRepository;
import com.foodai.backend.repository.UserSettingsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [BE-09] Diary persistence, user ownership and immutable nutrition snapshots. */
@Service
public class FoodDiaryService {
    private final DiaryMealRepository meals;
    private final DiaryItemRepository diaryItems;
    private final FoodRepository foods;
    private final RecognitionItemRepository recognitionItems;
    private final UserSettingsRepository settings;
    private final NutritionCalculator calculator;

    public FoodDiaryService(DiaryMealRepository meals, DiaryItemRepository diaryItems,
                            FoodRepository foods, RecognitionItemRepository recognitionItems,
                            UserSettingsRepository settings, NutritionCalculator calculator) {
        this.meals = meals;
        this.diaryItems = diaryItems;
        this.foods = foods;
        this.recognitionItems = recognitionItems;
        this.settings = settings;
        this.calculator = calculator;
    }

    @Transactional
    public DiaryResponse.Meal create(User user, CreateDiaryMealRequest request) {
        ZoneId zone = zone(user);
        DiaryMeal meal = new DiaryMeal();
        meal.setUser(user);
        meal.setMealType(request.mealType());
        meal.setEatenAtUtc(utc(request.eatenAt()));
        meal.setLocalMealDate(request.eatenAt().atZoneSameInstant(zone).toLocalDate());
        meal.setNotes(request.notes());
        boolean recognized = request.items().stream().anyMatch(i -> i.recognitionItemId() != null);
        meal.setSourceType(recognized ? DiaryMeal.SourceType.RECOGNITION : DiaryMeal.SourceType.MANUAL);
        meal = meals.save(meal);

        Set<Long> usedRecognitionItems = new HashSet<>();
        for (CreateDiaryMealRequest.Item requestItem : request.items()) {
            if ((requestItem.foodId() == null) == (requestItem.recognitionItemId() == null)) {
                throw bad("Exactly one of foodId or recognitionItemId must be provided per item.");
            }
            RecognitionItem recognition = null;
            Food food;
            BigDecimal grams = requestItem.consumedGrams();
            BigDecimal multiplier = requestItem.portionMultiplier();
            if (requestItem.recognitionItemId() != null) {
                Long id = requestItem.recognitionItemId();
                if (!usedRecognitionItems.add(id)) {
                    throw bad("Duplicate recognitionItemId in one meal.");
                }
                // Lock the recognition row to serialize two requests attempting to log the same item.
                recognition = recognitionItems.findLockedById(id)
                        .orElseThrow(() -> notFound("Recognition item not found."));
                if (!user.getId().equals(recognition.getSession().getUser().getId())) {
                    throw notFound("Recognition item not found.");
                }
                if (recognition.getSession().getStatus() != RecognitionSession.Status.COMPLETED ||
                        recognition.getItemStatus() != RecognitionItem.ItemStatus.CONFIRMED ||
                        recognition.getSelectedFood() == null) {
                    throw new FoodApiException(HttpStatus.CONFLICT, "Recognition item must be confirmed first.");
                }
                if (diaryItems.existsByRecognitionItemId(id)) {
                    throw new FoodApiException(HttpStatus.CONFLICT, "Recognition item is already in the diary.");
                }
                food = recognition.getSelectedFood();
                if (grams == null && multiplier == null) {
                    grams = recognition.getEstimatedGrams();
                    multiplier = recognition.getPortionMultiplier();
                }
            } else {
                food = foods.findById(requestItem.foodId())
                        .orElseThrow(() -> notFound("Food not found."));
            }
            requirePublished(food);
            grams = calculateGrams(food, grams, multiplier);
            multiplier = calculator.multiplier(grams, food, multiplier);
            validateStorageAmounts(grams, multiplier);
            DiaryItem entry = new DiaryItem();
            entry.setMeal(meal);
            entry.setFood(food);
            entry.setRecognitionItem(recognition);
            writeNutritionSnapshot(entry, food, grams, multiplier);
            diaryItems.save(entry);
        }
        return detail(meal);
    }

    @Transactional(readOnly = true)
    public DiaryResponse.Meal get(User user, Long mealId) {
        return detail(ownedMeal(user, mealId));
    }

    @Transactional(readOnly = true)
    public DiaryResponse.History history(User user, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw bad("Date range must be valid and at most 366 days.");
        }
        List<DiaryMeal> result = meals.findByUserIdAndLocalMealDateBetweenOrderByEatenAtUtcDesc(
                user.getId(), from, to);
        List<Long> ids = result.stream().map(DiaryMeal::getId).toList();
        Map<Long, List<DiaryItem>> itemIndex = new HashMap<>();
        if (!ids.isEmpty()) {
            for (DiaryItem item : diaryItems.findByMealIdInOrderByIdAsc(ids)) {
                itemIndex.computeIfAbsent(item.getMeal().getId(), key -> new ArrayList<>()).add(item);
            }
        }
        List<DiaryResponse.Meal> details = result.stream()
                .map(meal -> toResponse(meal, itemIndex.getOrDefault(meal.getId(), List.of())))
                .toList();
        DiaryResponse.Nutrition total = NutritionCalculator.zero();
        for (DiaryResponse.Meal entry : details) total = NutritionCalculator.plus(total, entry.total());
        return new DiaryResponse.History(from, to, total, details);
    }

    @Transactional
    public DiaryResponse.Meal updateMeal(User user, Long mealId, UpdateDiaryMealRequest request) {
        if (request.mealType() == null && request.eatenAt() == null && request.notes() == null) {
            throw bad("Provide mealType, eatenAt or notes to update a meal.");
        }
        DiaryMeal meal = ownedMeal(user, mealId);
        if (request.mealType() != null) meal.setMealType(request.mealType());
        if (request.eatenAt() != null) {
            meal.setEatenAtUtc(utc(request.eatenAt()));
            meal.setLocalMealDate(request.eatenAt().atZoneSameInstant(zone(user)).toLocalDate());
        }
        if (request.notes() != null) meal.setNotes(request.notes());
        return detail(meals.save(meal));
    }

    @Transactional
    public DiaryResponse.Meal updateItem(User user, Long mealId, Long itemId,
                                         UpdateDiaryItemRequest request) {
        DiaryMeal meal = ownedMeal(user, mealId);
        if (request.foodId() == null && request.consumedGrams() == null &&
                request.portionMultiplier() == null) {
            throw bad("Provide foodId, consumedGrams or portionMultiplier to update an item.");
        }
        DiaryItem item = diaryItems.findByIdAndMealId(itemId, mealId)
                .orElseThrow(() -> notFound("Diary item not found."));
        Food food = request.foodId() == null ? item.getFood() : foods.findById(request.foodId())
                .orElseThrow(() -> notFound("Food not found."));
        if (food == null) {
            throw new FoodApiException(HttpStatus.CONFLICT,
                    "Original food was removed; provide foodId to update this item.");
        }
        requirePublished(food);
        BigDecimal multiplier = request.portionMultiplier();
        BigDecimal grams = request.consumedGrams();
        if (grams == null && multiplier == null) grams = item.getConsumedGrams();
        grams = calculateGrams(food, grams, multiplier);
        multiplier = calculator.multiplier(grams, food, multiplier);
        validateStorageAmounts(grams, multiplier);
        item.setFood(food);
        writeNutritionSnapshot(item, food, grams, multiplier);
        diaryItems.save(item);
        return detail(meal);
    }

    @Transactional
    public void delete(User user, Long mealId) {
        DiaryMeal meal = ownedMeal(user, mealId);
        // Database FK: diary_items.meal_id ON DELETE CASCADE.
        meals.delete(meal);
    }

    private DiaryResponse.Meal detail(DiaryMeal meal) {
        return toResponse(meal, diaryItems.findByMealIdOrderByIdAsc(meal.getId()));
    }

    private DiaryResponse.Meal toResponse(DiaryMeal meal, List<DiaryItem> entries) {
        List<DiaryResponse.Item> responseItems = new ArrayList<>();
        DiaryResponse.Nutrition total = NutritionCalculator.zero();
        for (DiaryItem entry : entries) {
            DiaryResponse.Nutrition value = new DiaryResponse.Nutrition(
                    entry.getCaloriesKcal(), entry.getProteinG(), entry.getCarbsG(), entry.getFatG());
            total = NutritionCalculator.plus(total, value);
            responseItems.add(new DiaryResponse.Item(entry.getId(),
                    entry.getFood() == null ? null : entry.getFood().getId(),
                    entry.getRecognitionItem() == null ? null : entry.getRecognitionItem().getId(),
                    entry.getFoodNameSnapshot(), entry.getServingLabelSnapshot(),
                    entry.getPortionMultiplier(), entry.getConsumedGrams(), value));
        }
        return new DiaryResponse.Meal(meal.getId(), meal.getMealType(), meal.getEatenAtUtc(),
                meal.getLocalMealDate(), meal.getSourceType(), meal.getNotes(), total, responseItems);
    }

    private DiaryMeal ownedMeal(User user, Long id) {
        return meals.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> notFound("Diary meal not found."));
    }

    private void writeNutritionSnapshot(DiaryItem entry, Food food,
                                        BigDecimal grams, BigDecimal multiplier) {
        DiaryResponse.Nutrition nutrition = calculator.forGrams(food, grams);
        BigDecimal limit = new BigDecimal("99999999.99");
        if (nutrition.caloriesKcal().compareTo(limit) > 0 ||
                nutrition.proteinG().compareTo(limit) > 0 ||
                nutrition.carbsG().compareTo(limit) > 0 || nutrition.fatG().compareTo(limit) > 0) {
            throw bad("Nutrition amount exceeds supported database range.");
        }
        entry.setFoodNameSnapshot(food.getNameVi());
        entry.setServingLabelSnapshot(food.getStandardServingLabel());
        entry.setPortionMultiplier(multiplier);
        entry.setConsumedGrams(grams);
        entry.setCaloriesKcal(nutrition.caloriesKcal());
        entry.setProteinG(nutrition.proteinG());
        entry.setCarbsG(nutrition.carbsG());
        entry.setFatG(nutrition.fatG());
    }

    private static void requirePublished(Food food) {
        if (food.getCatalogStatus() != Food.CatalogStatus.PUBLISHED) {
            throw bad("Only published foods can be logged.");
        }
    }

    private BigDecimal calculateGrams(Food food, BigDecimal grams, BigDecimal multiplier) {
        BigDecimal answer = calculator.grams(food, grams, multiplier);
        if (answer.signum() <= 0) throw bad("Portion is too small to store in grams.");
        return answer.setScale(2, RoundingMode.HALF_UP);
    }

    private static void validateStorageAmounts(BigDecimal grams, BigDecimal portion) {
        if (grams.signum() <= 0 || grams.compareTo(new BigDecimal("9999999.99")) > 0 ||
                portion.signum() <= 0 || portion.compareTo(new BigDecimal("999999.999")) > 0) {
            throw bad("Portion exceeds supported database range.");
        }
    }

    private ZoneId zone(User user) {
        String name = settings.findById(user.getId())
                .map(x -> x.getTimezoneName()).orElse("Asia/Ho_Chi_Minh");
        try { return ZoneId.of(name); }
        catch (DateTimeException | NullPointerException ex) {
            throw new FoodApiException(HttpStatus.CONFLICT, "Invalid user timezone setting.");
        }
    }

    private static LocalDateTime utc(OffsetDateTime timestamp) {
        return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
    }

    private static FoodApiException notFound(String message) {
        return new FoodApiException(HttpStatus.NOT_FOUND, message);
    }
    private static FoodApiException bad(String message) {
        return new FoodApiException(HttpStatus.BAD_REQUEST, message);
    }
}
