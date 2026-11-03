package com.foodai.backend.service;

import com.foodai.backend.controller.AiRecognitionResultsController;
import com.foodai.backend.dto.request.AiRecognitionResultsRequest;
import com.foodai.backend.exception.FoodApiException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiCallbackTest {
    @Test
    void goodCallbackKeyDelegatesResults() {
        RecognitionResultIngestionService service = mock(RecognitionResultIngestionService.class);
        String key = "this-is-a-long-test-only-internal-key-123456";
        AiRecognitionResultsController controller = new AiRecognitionResultsController(service, key);
        AiRecognitionResultsRequest request = new AiRecognitionResultsRequest("yolo-v1", List.of());
        controller.save(1L, key, request);
        verify(service).saveAiResults(eq(1L), eq("yolo-v1"), eq(List.of()));
    }

    @Test
    void badCallbackKeyIsRejectedBeforeDatabaseWrites() {
        RecognitionResultIngestionService service = mock(RecognitionResultIngestionService.class);
        AiRecognitionResultsController controller = new AiRecognitionResultsController(service,
                "this-is-a-long-test-only-internal-key-123456");
        AiRecognitionResultsRequest request = new AiRecognitionResultsRequest("yolo-v1",
                List.of(new AiRecognitionResultsRequest.Detection("pho", new BigDecimal("0.8"),
                        new BigDecimal("100"), null, null, null, null)));
        FoodApiException ex = assertThrows(FoodApiException.class,
                () -> controller.save(1L, "invalid", request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verifyNoInteractions(service);
    }
}
