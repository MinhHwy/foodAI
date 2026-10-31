package com.foodai.backend.service;

import com.foodai.backend.recognition.AiRecognitionClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
public class RecognitionService {

    private final AiRecognitionClient aiRecognitionClient;

    public RecognitionService(AiRecognitionClient aiRecognitionClient) {
        this.aiRecognitionClient = aiRecognitionClient;
    }

    public JsonNode recognize(byte[] image, MediaType contentType) {
        return aiRecognitionClient.recognize(image, contentType);
    }
}
