package com.foodai.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

/** Metadata about an image already uploaded through BE-05. Does not run AI inference. */
public record CreateRecognitionSessionRequest(
        @NotBlank @Size(max = 1024) String imageUrl,
        @Size(max = 255) String cloudinaryPublicId,
        @Size(max = 100) String mimeType,
        @Positive Long fileSizeBytes) {}
