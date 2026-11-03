package com.foodai.backend.controller;

import com.foodai.backend.dto.request.AiRecognitionResultsRequest;
import com.foodai.backend.dto.response.ApiResponse;
import com.foodai.backend.exception.FoodApiException;
import com.foodai.backend.service.RecognitionResultIngestionService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal callback: configured service-to-service key is mandatory. */
@RestController
@RequestMapping("/api/v1/internal/recognitions")
public class AiRecognitionResultsController {
    private final RecognitionResultIngestionService results;
    private final String key;

    public AiRecognitionResultsController(RecognitionResultIngestionService results,
                                         @Value("${FOODAI_AI_INTERNAL_KEY:}") String key) {
        this.results = results;
        this.key = key;
    }

    @PostMapping("/{sessionId}/results")
    public ApiResponse<Void> save(@PathVariable Long sessionId,
            @RequestHeader(value = "X-FoodAI-AI-Key", required = false) String providedKey,
            @Valid @RequestBody AiRecognitionResultsRequest request) {
        if (key == null || key.length() < 32) {
            throw new FoodApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI callback is disabled until FOODAI_AI_INTERNAL_KEY is configured (32+ characters).");
        }
        if (providedKey == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new FoodApiException(HttpStatus.UNAUTHORIZED, "Invalid AI service credential.");
        }
        List<RecognitionResultIngestionService.Prediction> predictions = request.predictions()
                .stream().map(p -> new RecognitionResultIngestionService.Prediction(
                        p.label(), p.confidence(), p.estimatedGrams(), p.bboxX(), p.bboxY(),
                        p.bboxWidth(), p.bboxHeight())).toList();
        results.saveAiResults(sessionId, request.modelVersion(), predictions);
        return ApiResponse.success("Trusted AI results saved.", null);
    }
}
