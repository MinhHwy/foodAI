package com.foodai.backend.service;

import com.foodai.backend.entity.RecognitionSession;
import com.foodai.backend.exception.FoodApiException;
import com.foodai.backend.repository.ModelFoodClassRepository;
import com.foodai.backend.repository.RecognitionItemRepository;
import com.foodai.backend.repository.RecognitionSessionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecognitionResultIngestionServiceTest {
    private static RecognitionSession uploadedSession() {
        RecognitionSession session = new RecognitionSession();
        session.setStatus(RecognitionSession.Status.UPLOADED);
        return session;
    }

    @Test
    void completesSessionEvenWhenAiFoundNoFood() {
        RecognitionSessionRepository sessions = mock(RecognitionSessionRepository.class);
        RecognitionItemRepository items = mock(RecognitionItemRepository.class);
        ModelFoodClassRepository classes = mock(ModelFoodClassRepository.class);
        RecognitionSession session = uploadedSession();
        when(sessions.findLockedById(9L)).thenReturn(Optional.of(session));
        new RecognitionResultIngestionService(sessions, items, classes)
                .saveAiResults(9L, "yolo-food-v1", List.of());
        assertEquals(RecognitionSession.Status.COMPLETED, session.getStatus());
        verify(sessions).save(session);
        verify(items, never()).save(any());
    }

    @Test
    void rejectsBboxThatWouldNeedDatabaseRounding() {
        RecognitionSessionRepository sessions = mock(RecognitionSessionRepository.class);
        RecognitionItemRepository items = mock(RecognitionItemRepository.class);
        ModelFoodClassRepository classes = mock(ModelFoodClassRepository.class);
        when(sessions.findLockedById(9L)).thenReturn(Optional.of(uploadedSession()));
        var detection = new RecognitionResultIngestionService.Prediction(
                "pho", new BigDecimal("0.91"), new BigDecimal("120"),
                new BigDecimal("0.1234567"), new BigDecimal("0.2"),
                new BigDecimal("0.2"), new BigDecimal("0.3"));
        FoodApiException ex = assertThrows(FoodApiException.class, () ->
                new RecognitionResultIngestionService(sessions, items, classes)
                        .saveAiResults(9L, "yolo-food-v1", List.of(detection)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(items, never()).save(any());
    }
}
