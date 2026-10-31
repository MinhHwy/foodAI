package com.foodai.backend.recognition;

import com.foodai.backend.config.AiServiceProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public class RestClientAiRecognitionClient implements AiRecognitionClient {

    private static final String NOT_CONFIGURED_MESSAGE =
            "AI recognition is unavailable until the AI service contract and endpoint are configured.";

    private final AiServiceProperties properties;
    private final RestClient restClient;

    public RestClientAiRecognitionClient(AiServiceProperties properties) {
        this.properties = properties;
        properties.validateForMultipart();
        this.restClient = properties.isConfiguredForMultipart() ? buildRestClient(properties) : null;
    }

    RestClientAiRecognitionClient(AiServiceProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public JsonNode recognize(byte[] image, MediaType contentType) {
        if (restClient == null) {
            throw new AiRecognitionException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_SERVICE_NOT_CONFIGURED",
                    NOT_CONFIGURED_MESSAGE);
        }
        if (image == null || image.length == 0 || contentType == null) {
            throw new IllegalArgumentException("A non-empty image and content type are required.");
        }

        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        multipart.part(properties.imagePartName(), new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return "food-image";
            }
        }).contentType(contentType);

        try {
            JsonNode response = restClient.post()
                    .uri(properties.recognitionPath())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart.build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.isNull()) {
                throw invalidResponse();
            }
            return response;
        } catch (AiRecognitionException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new AiRecognitionException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_SERVICE_ERROR",
                    "The AI service returned an unsuccessful response.",
                    exception);
        } catch (ResourceAccessException exception) {
            if (hasTimeoutCause(exception)) {
                throw new AiRecognitionException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "AI_SERVICE_TIMEOUT",
                        "The AI service did not respond before the configured timeout.",
                        exception);
            }
            throw new AiRecognitionException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_SERVICE_UNAVAILABLE",
                    "The AI service could not be reached.",
                    exception);
        } catch (RestClientException exception) {
            throw new AiRecognitionException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_SERVICE_INVALID_RESPONSE",
                    "The AI service returned a response that could not be processed.",
                    exception);
        }
    }

    private static RestClient buildRestClient(AiServiceProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.responseTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private static AiRecognitionException invalidResponse() {
        return new AiRecognitionException(
                HttpStatus.BAD_GATEWAY,
                "AI_SERVICE_INVALID_RESPONSE",
                "The AI service returned an empty response.");
    }

    private static boolean hasTimeoutCause(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException
                    || cause instanceof HttpTimeoutException
                    || cause instanceof TimeoutException) {
                return true;
            }
        }
        return false;
    }

}
