package com.foodai.backend.service;

import com.foodai.backend.dto.request.ConfirmRecognitionItemRequest;
import com.foodai.backend.dto.request.CreateRecognitionSessionRequest;
import com.foodai.backend.dto.response.RecognitionResponse;
import com.foodai.backend.entity.Food;
import com.foodai.backend.entity.RecognitionFeedback;
import com.foodai.backend.entity.RecognitionItem;
import com.foodai.backend.entity.RecognitionSession;
import com.foodai.backend.entity.User;
import com.foodai.backend.exception.FoodApiException;
import com.foodai.backend.repository.FoodRepository;
import com.foodai.backend.repository.RecognitionFeedbackRepository;
import com.foodai.backend.repository.RecognitionItemRepository;
import com.foodai.backend.repository.RecognitionSessionRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecognitionService {
    private final RecognitionSessionRepository sessions;
    private final RecognitionItemRepository items;
    private final RecognitionFeedbackRepository feedback;
    private final FoodRepository foods;
    private final NutritionCalculator calculator;

    public RecognitionService(RecognitionSessionRepository sessions,
                              RecognitionItemRepository items,
                              RecognitionFeedbackRepository feedback,
                              FoodRepository foods, NutritionCalculator calculator) {
        this.sessions = sessions;
        this.items = items;
        this.feedback = feedback;
        this.foods = foods;
        this.calculator = calculator;
    }

    @Transactional
    public RecognitionResponse create(CreateRecognitionSessionRequest request, User user) {
        try {
            URI uri = URI.create(request.imageUrl());
            if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                    !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())) {
                throw new IllegalArgumentException("Not a Cloudinary HTTPS image URL");
            }
        } catch (IllegalArgumentException ex) {
            throw new FoodApiException(HttpStatus.BAD_REQUEST,
                    "imageUrl must be a valid HTTPS Cloudinary URL from BE-05.");
        }
        if (request.fileSizeBytes() != null &&
                (request.fileSizeBytes() < 1 || request.fileSizeBytes() > 10485760)) {
            throw new FoodApiException(HttpStatus.BAD_REQUEST, "Image size must be 1..10485760 bytes.");
        }
        RecognitionSession session = new RecognitionSession();
        session.setUser(user);
        session.setImageUrl(request.imageUrl());
        session.setCloudinaryPublicId(request.cloudinaryPublicId());
        session.setMimeType(request.mimeType());
        session.setFileSizeBytes(request.fileSizeBytes());
        session.setStatus(RecognitionSession.Status.UPLOADED);
        return new RecognitionResponse(sessions.save(session).getId(), session.getStatus(),
                session.getImageUrl(), null, List.of());
    }

    @Transactional(readOnly = true)
    public RecognitionResponse get(Long sessionId, User user) {
        RecognitionSession session = sessions.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new FoodApiException(HttpStatus.NOT_FOUND, "Recognition session not found."));
        return response(session);
    }

    @Transactional
    public RecognitionResponse confirm(Long sessionId, Long itemId,
                                       ConfirmRecognitionItemRequest request, User user) {
        RecognitionSession session = sessions.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new FoodApiException(HttpStatus.NOT_FOUND, "Recognition session not found."));
        if (session.getStatus() != RecognitionSession.Status.COMPLETED) {
            throw new FoodApiException(HttpStatus.CONFLICT, "Recognition session has not completed.");
        }
        RecognitionItem item = items.findLockedById(itemId)
                .orElseThrow(() -> new FoodApiException(HttpStatus.NOT_FOUND, "Recognition item not found."));
        if (!sessionId.equals(item.getSession().getId())) {
            throw new FoodApiException(HttpStatus.NOT_FOUND, "Recognition item not found in session.");
        }
        if (request.status() != RecognitionItem.ItemStatus.CONFIRMED &&
                request.status() != RecognitionItem.ItemStatus.REJECTED) {
            throw new FoodApiException(HttpStatus.BAD_REQUEST, "status must be CONFIRMED or REJECTED.");
        }
        if (item.getItemStatus() != RecognitionItem.ItemStatus.PREDICTED) {
            throw new FoodApiException(HttpStatus.CONFLICT, "Recognition item has already been reviewed.");
        }

        Food previousFood = item.getSelectedFood() != null ? item.getSelectedFood() : item.getPredictedFood();
        BigDecimal previousPortion = item.getPortionMultiplier();
        Food selected = null;
        BigDecimal portion = null;

        if (request.status() == RecognitionItem.ItemStatus.CONFIRMED) {
            Long foodId = request.foodId() != null ? request.foodId() :
                    (item.getPredictedFood() != null ? item.getPredictedFood().getId() : null);
            if (foodId == null) {
                throw new FoodApiException(HttpStatus.BAD_REQUEST, "foodId is required for an unmapped label.");
            }
            selected = foods.findById(foodId)
                    .orElseThrow(() -> new FoodApiException(HttpStatus.NOT_FOUND, "Food not found."));
            if (selected.getCatalogStatus() != Food.CatalogStatus.PUBLISHED) {
                throw new FoodApiException(HttpStatus.BAD_REQUEST, "Only published foods may be selected.");
            }
            BigDecimal requestedPortion = request.portionMultiplier() != null ?
                    request.portionMultiplier() : item.getPortionMultiplier();
            BigDecimal grams = request.consumedGrams() != null ? request.consumedGrams() :
                    (request.portionMultiplier() != null ? null : item.getEstimatedGrams());
            if (grams == null && requestedPortion == null) requestedPortion = BigDecimal.ONE;
            grams = calculator.grams(selected, grams, requestedPortion);
            portion = calculator.multiplier(grams, selected, requestedPortion);
            if (grams.signum() <= 0 || grams.compareTo(new BigDecimal("9999999.99")) > 0 ||
                    portion.signum() <= 0 || portion.compareTo(new BigDecimal("999999.999")) > 0) {
                throw new FoodApiException(HttpStatus.BAD_REQUEST, "Portion exceeds supported range.");
            }
            item.setSelectedFood(selected);
            item.setPortionMultiplier(portion);
            item.setEstimatedGrams(grams);
            item.setPortionMethod(RecognitionItem.PortionMethod.MANUAL);
        } else {
            if (request.foodId() != null || request.portionMultiplier() != null || request.consumedGrams() != null) {
                throw new FoodApiException(HttpStatus.BAD_REQUEST, "REJECTED does not accept food or portion values.");
            }
            item.setSelectedFood(null);
        }
        item.setItemStatus(request.status());
        items.save(item);

        boolean changedFood = !Objects.equals(
                previousFood == null ? null : previousFood.getId(),
                selected == null ? null : selected.getId());
        boolean changedPortion = previousPortion != null && portion != null &&
                previousPortion.compareTo(portion) != 0;
        if (changedFood || changedPortion ||
                (request.correctionNote() != null && !request.correctionNote().isBlank())) {
            RecognitionFeedback change = new RecognitionFeedback();
            change.setRecognitionItem(item);
            change.setUser(user);
            change.setPreviousFood(previousFood);
            change.setCorrectedFood(selected);
            change.setPreviousPortionMultiplier(previousPortion);
            change.setCorrectedPortionMultiplier(portion);
            change.setCorrectionNote(request.correctionNote());
            feedback.save(change);
        }
        return response(session);
    }

    private RecognitionResponse response(RecognitionSession session) {
        List<RecognitionResponse.Item> result = items.findBySessionIdOrderByIdAsc(session.getId())
                .stream().map(i -> new RecognitionResponse.Item(
                        i.getId(), i.getPredictedLabel(), id(i.getPredictedFood()),
                        i.getPredictedFood() == null ? null : i.getPredictedFood().getNameVi(),
                        i.getConfidenceScore(), id(i.getSelectedFood()),
                        i.getSelectedFood() == null ? null : i.getSelectedFood().getNameVi(),
                        i.getPortionMultiplier(), i.getEstimatedGrams(), i.getItemStatus()))
                .toList();
        return new RecognitionResponse(session.getId(), session.getStatus(),
                session.getImageUrl(), session.getModelVersion(), result);
    }

    private static Long id(Food food) { return food == null ? null : food.getId(); }
}
