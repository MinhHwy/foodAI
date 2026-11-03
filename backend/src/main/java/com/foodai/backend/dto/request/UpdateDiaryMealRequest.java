package com.foodai.backend.dto.request;

import com.foodai.backend.entity.MealType;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/** Partial update of editable metadata. Cannot change diary ownership or source. */
public record UpdateDiaryMealRequest(
        MealType mealType,
        OffsetDateTime eatenAt,
        @Size(max = 500) String notes) {}
