package com.foodai.backend.exception;

import org.springframework.http.HttpStatus;

public class FoodApiException extends RuntimeException {
    private final HttpStatus status;
    public FoodApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
}
