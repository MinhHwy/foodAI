package com.foodai.backend.config;

import com.foodai.backend.recognition.AiRecognitionClient;
import com.foodai.backend.recognition.RestClientAiRecognitionClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiServiceProperties.class)
public class AiServiceConfiguration {

    @Bean
    public AiRecognitionClient aiRecognitionClient(AiServiceProperties properties) {
        return new RestClientAiRecognitionClient(properties);
    }
}
