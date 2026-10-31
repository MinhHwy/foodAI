package com.foodai.backend.controller;

import com.foodai.backend.recognition.AiRecognitionException;
import com.foodai.backend.service.RecognitionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/recognitions")
public class RecognitionController {

    private final RecognitionService recognitionService;
    private final long maxImageBytes;

    public RecognitionController(
            RecognitionService recognitionService,
            @Value("${foodai.recognition.max-image-bytes:10485760}") long maxImageBytes) {
        this.recognitionService = recognitionService;
        if (maxImageBytes <= 0) {
            throw new IllegalStateException("Recognition image size limit must be greater than zero.");
        }
        this.maxImageBytes = maxImageBytes;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JsonNode recognize(@RequestPart("image") MultipartFile image) {
        if (image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required.");
        }
        if (image.getSize() > maxImageBytes) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "The image exceeds the upload limit.");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "An image content type is required.");
        }

        try {
            return recognitionService.recognize(image.getBytes(), MediaType.parseMediaType(contentType));
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded image could not be read.");
        }
    }

    @ExceptionHandler(AiRecognitionException.class)
    public ResponseEntity<ProblemDetail> handleAiRecognitionException(AiRecognitionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle(exception.getStatus().getReasonPhrase());
        problem.setProperty("code", exception.getCode());
        return ResponseEntity.status(exception.getStatus()).body(problem);
    }
}
