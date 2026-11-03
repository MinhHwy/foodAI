package com.foodai.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.foodai.backend.auth.JwtTokenService;
import com.foodai.backend.auth.SecurityConfig;
import com.foodai.backend.repository.UserRepository;
import com.foodai.backend.service.RecognitionResultIngestionService;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {HealthController.class, AiRecognitionResultsController.class}, properties = {
        "FOODAI_AI_INTERNAL_KEY=test-only-internal-key-1234567890123456"
})
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RecognitionResultIngestionService recognitionResultIngestionService;

    @Test
    void healthEndpointReturnsExpectedResponse() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("FoodAI Backend is running"));
    }

    @Test
    void aiCallbackReachesItsOwnApiKeyCheckWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/v1/internal/recognitions/12/results")
                        .header("X-FoodAI-AI-Key", "test-only-internal-key-1234567890123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelVersion\":\"test\",\"predictions\":[]}"))
                .andExpect(status().isOk());

        verify(recognitionResultIngestionService).saveAiResults(12L, "test", java.util.List.of());
    }

    @Test
    void diaryRecognitionAndImageManagementRequireJwt() throws Exception {
        mockMvc.perform(get("/api/v1/diary/meals/12"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/recognitions/12"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/images/test-public-id"))
                .andExpect(status().isUnauthorized());
    }

}
