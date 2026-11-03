package com.foodai.backend.controller;

import com.foodai.backend.dto.request.CreateDiaryMealRequest;
import com.foodai.backend.dto.request.UpdateDiaryItemRequest;
import com.foodai.backend.dto.request.UpdateDiaryMealRequest;
import com.foodai.backend.dto.response.ApiResponse;
import com.foodai.backend.dto.response.DiaryResponse;
import com.foodai.backend.service.CurrentFoodUser;
import com.foodai.backend.service.FoodDiaryService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** BE-09. No userId parameter is trusted: ownership comes from verified principal. */
@RestController
@RequestMapping("/api/v1/diary")
public class FoodDiaryController {
    private final FoodDiaryService service;
    private final CurrentFoodUser users;
    public FoodDiaryController(FoodDiaryService service, CurrentFoodUser users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping("/meals")
    public ResponseEntity<ApiResponse<DiaryResponse.Meal>> create(
            @Valid @RequestBody CreateDiaryMealRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Meal logged.",
                service.create(users.require(principal), request)));
    }

    @GetMapping("/meals/{mealId}")
    public ApiResponse<DiaryResponse.Meal> get(@PathVariable Long mealId, Principal principal) {
        return ApiResponse.success("Meal loaded.", service.get(users.require(principal), mealId));
    }

    @GetMapping("/daily")
    public ApiResponse<DiaryResponse.History> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Principal principal) {
        return ApiResponse.success("Daily diary loaded.",
                service.history(users.require(principal), date, date));
    }

    @GetMapping("/history")
    public ApiResponse<DiaryResponse.History> history(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {
        return ApiResponse.success("Diary history loaded.",
                service.history(users.require(principal), from, to));
    }

    @PatchMapping("/meals/{mealId}")
    public ApiResponse<DiaryResponse.Meal> updateMeal(@PathVariable Long mealId,
            @Valid @RequestBody UpdateDiaryMealRequest request, Principal principal) {
        return ApiResponse.success("Meal updated.",
                service.updateMeal(users.require(principal), mealId, request));
    }

    @PutMapping("/meals/{mealId}/items/{itemId}")
    public ApiResponse<DiaryResponse.Meal> updateItem(@PathVariable Long mealId,
            @PathVariable Long itemId, @Valid @RequestBody UpdateDiaryItemRequest request,
            Principal principal) {
        return ApiResponse.success("Diary item updated.",
                service.updateItem(users.require(principal), mealId, itemId, request));
    }

    @DeleteMapping("/meals/{mealId}")
    public ApiResponse<Void> delete(@PathVariable Long mealId, Principal principal) {
        service.delete(users.require(principal), mealId);
        return ApiResponse.success("Meal deleted.", null);
    }
}
