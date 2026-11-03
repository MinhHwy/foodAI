package com.foodai.backend.dto.request;

import com.foodai.backend.entity.RecognitionItem.ItemStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ConfirmRecognitionItemRequest(
        @NotNull ItemStatus status,
        @Positive Long foodId,
        @DecimalMin(value = "0.001") @DecimalMax(value = "999999.999") BigDecimal portionMultiplier,
        @DecimalMin(value = "0.01") @DecimalMax(value = "9999999.99") BigDecimal consumedGrams,
        @Size(max = 500) String correctionNote) {}
