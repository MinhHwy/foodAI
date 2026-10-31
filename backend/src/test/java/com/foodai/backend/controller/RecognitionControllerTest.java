package com.foodai.backend.controller;

import com.foodai.backend.config.AiServiceProperties;
import com.foodai.backend.recognition.RestClientAiRecognitionClient;
import com.foodai.backend.service.RecognitionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecognitionControllerTest {

    @Test
    void reportsUnavailableInsteadOfReturningAFakeRecognitionWhenAiIsNotConfigured() throws Exception {
        AiServiceProperties properties = new AiServiceProperties(
                "", "", "", "", Duration.ofSeconds(2), Duration.ofSeconds(30));
        RecognitionService service = new RecognitionService(new RestClientAiRecognitionClient(properties));
        RecognitionController controller = new RecognitionController(service, 10_485_760);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        MockMultipartFile image = new MockMultipartFile(
                "image", "meal.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/recognitions").file(image))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_NOT_CONFIGURED"));
    }
}
