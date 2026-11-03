package com.foodai.backend.service;

import com.foodai.backend.entity.Food;
import com.foodai.backend.entity.ModelFoodClass;
import com.foodai.backend.entity.RecognitionItem;
import com.foodai.backend.entity.RecognitionSession;
import com.foodai.backend.exception.FoodApiException;
import com.foodai.backend.repository.ModelFoodClassRepository;
import com.foodai.backend.repository.RecognitionItemRepository;
import com.foodai.backend.repository.RecognitionSessionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Internal BE-07 integration point for the TRUSTED server-side AI client.
 * Deliberately no public HTTP endpoint for injecting predicted labels/confidence.
 */
@Service
public class RecognitionResultIngestionService {
    public record Prediction(String label, BigDecimal confidence, BigDecimal estimatedGrams,
                             BigDecimal bboxX, BigDecimal bboxY,
                             BigDecimal bboxWidth, BigDecimal bboxHeight) {}

    private final RecognitionSessionRepository sessions;
    private final RecognitionItemRepository items;
    private final ModelFoodClassRepository classes;

    public RecognitionResultIngestionService(RecognitionSessionRepository sessions,
                                              RecognitionItemRepository items,
                                              ModelFoodClassRepository classes) {
        this.sessions = sessions;
        this.items = items;
        this.classes = classes;
    }

    @Transactional
    public void saveAiResults(Long sessionId, String modelVersion, List<Prediction> predictions) {
        if (modelVersion == null || modelVersion.isBlank() || modelVersion.length() > 100 ||
                predictions == null || predictions.size() > 50) {
            throw new FoodApiException(HttpStatus.BAD_REQUEST, "Invalid AI model version or predictions.");
        }
        RecognitionSession session = sessions.findLockedById(sessionId)
                .orElseThrow(() -> new FoodApiException(HttpStatus.NOT_FOUND, "Recognition session not found."));
        if ((session.getStatus() != RecognitionSession.Status.UPLOADED &&
                session.getStatus() != RecognitionSession.Status.PROCESSING) ||
                items.existsBySessionId(sessionId)) {
            throw new FoodApiException(HttpStatus.CONFLICT, "AI results have already been processed.");
        }
        for (Prediction p : predictions) {
            if (p == null || p.label() == null || p.label().isBlank() || p.label().length() > 200 ||
                    p.confidence() == null || p.confidence().compareTo(BigDecimal.ZERO) < 0 ||
                    p.confidence().compareTo(BigDecimal.ONE) > 0 ||
                    (p.estimatedGrams() != null && (p.estimatedGrams().signum() <= 0 ||
                    p.estimatedGrams().compareTo(new BigDecimal("9999999.99")) > 0 ||
                    p.estimatedGrams().setScale(2, RoundingMode.HALF_UP).signum() <= 0))) {
                throw new FoodApiException(HttpStatus.BAD_REQUEST, "Invalid AI prediction.");
            }
            checkBox(p);
            Food food = classes.findByModelVersionAndClassLabel(modelVersion, p.label())
                    .map(ModelFoodClass::getFood).orElse(null);
            RecognitionItem item = new RecognitionItem();
            item.setSession(session);
            item.setPredictedLabel(p.label());
            item.setPredictedFood(food);
            item.setConfidenceScore(p.confidence().setScale(5, RoundingMode.HALF_UP));
            item.setEstimatedGrams(p.estimatedGrams() == null ? null :
                    p.estimatedGrams().setScale(2, RoundingMode.HALF_UP));
            item.setBboxX(normalizedBbox(p.bboxX()));
            item.setBboxY(normalizedBbox(p.bboxY()));
            item.setBboxWidth(normalizedBbox(p.bboxWidth()));
            item.setBboxHeight(normalizedBbox(p.bboxHeight()));
            if (food != null && p.estimatedGrams() != null &&
                    food.getStandardServingGrams() != null && food.getStandardServingGrams().signum() > 0) {
                BigDecimal ratio = p.estimatedGrams().divide(
                        food.getStandardServingGrams(), 3, RoundingMode.HALF_UP);
                if (ratio.signum() > 0 && ratio.compareTo(new BigDecimal("999999.999")) <= 0)
                    item.setPortionMultiplier(ratio);
            }
            item.setPortionMethod(p.estimatedGrams() == null ?
                    RecognitionItem.PortionMethod.UNKNOWN : RecognitionItem.PortionMethod.AI_ESTIMATE);
            item.setItemStatus(RecognitionItem.ItemStatus.PREDICTED);
            items.save(item);
        }
        session.setModelVersion(modelVersion);
        session.setStatus(RecognitionSession.Status.COMPLETED);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        session.setFinishedAt(session.getCreatedAt() != null && now.isBefore(session.getCreatedAt())
                ? session.getCreatedAt() : now);
        sessions.save(session);
    }

    private static BigDecimal normalizedBbox(BigDecimal value) {
        return value == null ? null : value.setScale(6, RoundingMode.UNNECESSARY);
    }

    private static void checkBox(Prediction p) {
        BigDecimal[] v = {p.bboxX(), p.bboxY(), p.bboxWidth(), p.bboxHeight()};
        boolean allNull = true;
        boolean allPresent = true;
        for (BigDecimal n : v) { allNull &= n == null; allPresent &= n != null; }
        if (!allNull && !allPresent) {
            throw new FoodApiException(HttpStatus.BAD_REQUEST, "Incomplete detection bbox.");
        }
        // DECIMAL(7,6) stores at most six fractional digits. Reject values that
        // would round in the database and potentially invalidate bbox boundaries.
        for (BigDecimal n : v) {
            if (n != null) {
                try { normalizedBbox(n); }
                catch (ArithmeticException ex) {
                    throw new FoodApiException(HttpStatus.BAD_REQUEST,
                            "Detection bbox supports at most six digits after the decimal point.");
                }
            }
        }
        if (allPresent && (v[0].signum() < 0 || v[1].signum() < 0 ||
                v[2].signum() <= 0 || v[3].signum() <= 0 ||
                v[0].add(v[2]).compareTo(BigDecimal.ONE) > 0 ||
                v[1].add(v[3]).compareTo(BigDecimal.ONE) > 0)) {
            throw new FoodApiException(HttpStatus.BAD_REQUEST, "Invalid detection bbox (0..1).");
        }
    }
}
