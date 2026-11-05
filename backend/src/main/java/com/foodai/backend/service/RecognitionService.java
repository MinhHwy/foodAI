package com.foodai.backend.service;
import org.springframework.http.MediaType;
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
import com.foodai.backend.service.RecognitionResultIngestionService.Prediction;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RecognitionService {
    private static final Logger log = LoggerFactory.getLogger(RecognitionService.class);
    private static final String MOCK_MODEL_VERSION = "mock:foodai-demo-v1";

    private final RecognitionSessionRepository sessions;
    private final RecognitionItemRepository items;
    private final RecognitionFeedbackRepository feedback;
    private final FoodRepository foods;
    private final NutritionCalculator calculator;
    private final RecognitionResultIngestionService resultIngestion;
    private final RestClient aiClient;
    private final TransactionTemplate transactions;
    private final String aiBaseUrl;
    private final boolean useMock;
    private final boolean fallbackEnabled;

    public RecognitionService(RecognitionSessionRepository sessions,
                              RecognitionItemRepository items,
                              RecognitionFeedbackRepository feedback,
                              FoodRepository foods, NutritionCalculator calculator) {
        this.sessions = sessions;
        this.items = items;
        this.feedback = feedback;
        this.foods = foods;
        this.calculator = calculator;
        this.resultIngestion = null;
        this.aiClient = null;
        this.transactions = null;
        this.aiBaseUrl = null;
        this.useMock = false;
        this.fallbackEnabled = false;
    }

    @Autowired
    public RecognitionService(RecognitionSessionRepository sessions,
                              RecognitionItemRepository items,
                              RecognitionFeedbackRepository feedback,
                              FoodRepository foods, NutritionCalculator calculator,
                              RecognitionResultIngestionService resultIngestion,
                              PlatformTransactionManager transactionManager,
                              @Value("${foodai.ai.base-url:http://localhost:8000/predict}") String aiBaseUrl,
                              @Value("${foodai.ai.mock:false}") boolean useMock,
                              @Value("${foodai.ai.fallback.enabled:false}") boolean fallbackEnabled) {
        this.sessions = sessions;
        this.items = items;
        this.feedback = feedback;
        this.foods = foods;
        this.calculator = calculator;
        this.resultIngestion = resultIngestion;
        this.aiClient = buildAiClient();
        this.transactions = new TransactionTemplate(transactionManager);
        this.aiBaseUrl = aiBaseUrl;
        this.useMock = useMock;
        this.fallbackEnabled = fallbackEnabled;
    }

   private static RestClient buildAiClient() {
    HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(httpClient);

    requestFactory.setReadTimeout(Duration.ofSeconds(30));

    return RestClient.builder()
            .requestFactory(requestFactory)
            .build();
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
        RecognitionSession saved = sessions.save(session);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            Long sessionId = saved.getId();
            String imageUrl = saved.getImageUrl();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(() -> processSession(sessionId, imageUrl));
                }
            });
        }
        return new RecognitionResponse(saved.getId(), saved.getStatus(),
                saved.getImageUrl(), null, List.of(), false);
    }

    private void processSession(Long sessionId, String imageUrl) {
        try {
            transactions.executeWithoutResult(status -> {
                RecognitionSession session = sessions.findLockedById(sessionId)
                        .orElseThrow(() -> new IllegalStateException("Recognition session disappeared."));
                if (session.getStatus() != RecognitionSession.Status.UPLOADED) return;
                session.setStatus(RecognitionSession.Status.PROCESSING);
                sessions.save(session);
            });

            if (useMock) {
                completeWithMock(sessionId);
                return;
            }

           AiResult result = aiClient.post()
        .uri(aiBaseUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(java.util.Map.of(
                "sessionId", sessionId,
                "imageUrl", imageUrl
        ))
        .retrieve()
        .body(AiResult.class);
            if (result == null || result.modelVersion() == null || result.predictions() == null) {
                throw new IllegalStateException("AI service returned an incomplete response.");
            }
            List<Prediction> predictions = result.predictions().stream()
                    .map(p -> new Prediction(p.label(), p.confidence(), p.estimatedGrams(),
                            p.bboxX(), p.bboxY(), p.bboxWidth(), p.bboxHeight()))
                    .toList();
            resultIngestion.saveAiResults(sessionId, result.modelVersion(), predictions);
        } catch (Exception ex) {
            log.error("AI recognition failed for session {}.", sessionId, ex);
            if (fallbackEnabled) {
                try {
                    completeWithMock(sessionId);
                    return;
                } catch (Exception mockException) {
                    log.error("Mock recognition fallback failed for session {}.", sessionId, mockException);
                }
            }
            markFailed(sessionId);
        }
    }

    private void completeWithMock(Long sessionId) {
        log.error("MOCK RECOGNITION ENABLED for session {}: generated results are synthetic and are NOT real AI predictions.",
                sessionId);
        transactions.executeWithoutResult(status -> {
            RecognitionSession session = sessions.findLockedById(sessionId)
                    .orElseThrow(() -> new IllegalStateException("Recognition session disappeared."));
            if (session.getStatus() != RecognitionSession.Status.PROCESSING ||
                    items.existsBySessionId(sessionId)) {
                throw new IllegalStateException("Session cannot accept mock recognition results.");
            }
            List<Food> mockFoods = new ArrayList<>();
            foods.findById(1L).ifPresent(mockFoods::add);
            foods.findById(2L).ifPresent(mockFoods::add);
            if (mockFoods.isEmpty()) {
                throw new IllegalStateException("Mock recognition requires food record ID 1 or 2.");
            }
            for (Food food : mockFoods) {
                RecognitionItem item = new RecognitionItem();
                item.setSession(session);
                item.setPredictedLabel(food.getNameVi());
                item.setPredictedFood(food);
                item.setConfidenceScore(new BigDecimal("0.95000"));
                item.setEstimatedGrams(food.getStandardServingGrams() == null
                        ? new BigDecimal("100.00") : food.getStandardServingGrams());
                item.setPortionMultiplier(BigDecimal.ONE.setScale(3));
                item.setPortionMethod(RecognitionItem.PortionMethod.AI_ESTIMATE);
                item.setItemStatus(RecognitionItem.ItemStatus.PREDICTED);
                items.save(item);
            }
            session.setModelVersion(MOCK_MODEL_VERSION);
            session.setStatus(RecognitionSession.Status.COMPLETED);
            session.setFinishedAt(finishedAt(session));
            sessions.save(session);
        });
    }

    private void markFailed(Long sessionId) {
        try {
            transactions.executeWithoutResult(status -> sessions.findLockedById(sessionId).ifPresent(session -> {
                session.setStatus(RecognitionSession.Status.FAILED);
                session.setErrorCode("AI_SERVICE_FAILURE");
                session.setFinishedAt(finishedAt(session));
                sessions.save(session);
            }));
        } catch (Exception ex) {
            log.error("Could not mark recognition session {} as failed.", sessionId, ex);
        }
    }

    private static LocalDateTime finishedAt(RecognitionSession session) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return session.getCreatedAt() != null && now.isBefore(session.getCreatedAt())
                ? session.getCreatedAt() : now;
    }

    private record AiRequest(Long sessionId, String imageUrl) {}
    private record AiResult(String modelVersion, List<AiPrediction> predictions) {}
    private record AiPrediction(String label, BigDecimal confidence, BigDecimal estimatedGrams,
                                BigDecimal bboxX, BigDecimal bboxY,
                                BigDecimal bboxWidth, BigDecimal bboxHeight) {}

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
