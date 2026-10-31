package com.foodai.backend.recognition;

import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

public interface AiRecognitionClient {

    JsonNode recognize(byte[] image, MediaType contentType);
}
