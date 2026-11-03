package com.foodai.backend.dto.response;

import com.foodai.backend.entity.RecognitionItem;
import com.foodai.backend.entity.RecognitionSession;
import java.math.BigDecimal;
import java.util.List;

public record RecognitionResponse(
        Long sessionId, RecognitionSession.Status status, String imageUrl,
        String modelVersion, List<Item> items, boolean isMocked) {
    public RecognitionResponse(Long sessionId, RecognitionSession.Status status, String imageUrl,
                               String modelVersion, List<Item> items) {
        this(sessionId, status, imageUrl, modelVersion, items,
                modelVersion != null && modelVersion.startsWith("mock:"));
    }

    public record Item(
            Long id, String predictedLabel, Long predictedFoodId, String predictedFoodName,
            BigDecimal confidenceScore, Long selectedFoodId, String selectedFoodName,
            BigDecimal portionMultiplier, BigDecimal estimatedGrams,
            RecognitionItem.ItemStatus status) {}
}
