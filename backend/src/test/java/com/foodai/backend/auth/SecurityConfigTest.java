package com.foodai.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import tools.jackson.databind.ObjectMapper;

class SecurityConfigTest {
    @Test
    void corsUsesOnlyEnvironmentConfiguredOrigins() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/diary/daily");
        CorsConfiguration cors = new SecurityConfig(new ObjectMapper())
                .corsConfigurationSource("https://foodai.example, https://admin.foodai.example")
                .getCorsConfiguration(request);

        assertNotNull(cors);
        assertEquals(List.of("https://foodai.example", "https://admin.foodai.example"),
                cors.getAllowedOrigins());
        assertFalse(cors.getAllowedOrigins().contains("*"));
    }
}
