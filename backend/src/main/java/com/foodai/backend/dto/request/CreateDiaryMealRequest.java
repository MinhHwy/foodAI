package com.foodai.backend.dto.request;

import com.foodai.backend.entity.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** eatenAt includes an explicit offset, e.g. 2026-11-01T12:30:00+07:00. */
public record CreateDiaryMealRequest(
        @NotNull MealType mealType,
        @NotNull OffsetDateTime eatenAt,
        @Size(max = 500) String notes,
        @NotEmpty @Size(max = 50) List<@NotNull @Valid Item> items) {
    public record Item(
            @Positive Long foodId,
            @Positive Long recognitionItemId,
            @DecimalMin("0.001") @DecimalMax("999999.999") BigDecimal portionMultiplier,
            @DecimalMin("0.01") @DecimalMax("9999999.99") BigDecimal consumedGrams) {}
}
