package com.foodai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

@ConfigurationProperties(prefix = "ai.service")
public record AiServiceProperties(
        String baseUrl,
        String recognitionPath,
        String requestMode,
        String imagePartName,
        Duration connectTimeout,
        Duration responseTimeout) {

    public AiServiceProperties {
        baseUrl = normalize(baseUrl);
        recognitionPath = normalize(recognitionPath);
        requestMode = normalize(requestMode).toUpperCase(Locale.ROOT);
        imagePartName = normalize(imagePartName);
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        responseTimeout = responseTimeout == null ? Duration.ofSeconds(30) : responseTimeout;
    }

    public boolean isConfiguredForMultipart() {
        return !baseUrl.isEmpty()
                && !recognitionPath.isEmpty()
                && "MULTIPART".equals(requestMode)
                && !imagePartName.isEmpty();
    }

    public void validateForMultipart() {
        boolean hasAnyConfiguration = !baseUrl.isEmpty()
                || !recognitionPath.isEmpty()
                || !requestMode.isEmpty()
                || !imagePartName.isEmpty();
        if (!hasAnyConfiguration) {
            return;
        }
        if (!isConfiguredForMultipart()) {
            throw new IllegalStateException(
                    "Configure AI_SERVICE_BASE_URL, AI_SERVICE_RECOGNITION_PATH, "
                            + "AI_SERVICE_REQUEST_MODE=MULTIPART, and AI_SERVICE_IMAGE_PART_NAME together.");
        }

        URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("AI_SERVICE_BASE_URL must be a valid HTTP(S) URL.", exception);
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || !recognitionPath.startsWith("/")
                || recognitionPath.contains("://")
                || connectTimeout.isNegative()
                || connectTimeout.isZero()
                || responseTimeout.isNegative()
                || responseTimeout.isZero()) {
            throw new IllegalStateException("AI service URL, path, and timeouts are invalid.");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
