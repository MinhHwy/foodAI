package com.foodai.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateDiaryItemRequest(
        @Positive Long foodId,
        @DecimalMin("0.001") @DecimalMax("999999.999") BigDecimal portionMultiplier,
        @DecimalMin("0.01") @DecimalMax("9999999.99") BigDecimal consumedGrams) {}
