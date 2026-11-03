package com.foodai.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Payload from a TRUSTED AI service, never from the public frontend. */
public record AiRecognitionResultsRequest(
        @NotBlank @Size(max = 100) String modelVersion,
        @NotNull @Size(max = 50) List<@NotNull @Valid Detection> predictions) {
    public record Detection(
            @NotBlank @Size(max = 200) String label,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confidence,
            @DecimalMin("0.01") @DecimalMax("9999999.99") BigDecimal estimatedGrams,
            BigDecimal bboxX, BigDecimal bboxY,
            BigDecimal bboxWidth, BigDecimal bboxHeight) {}
}
