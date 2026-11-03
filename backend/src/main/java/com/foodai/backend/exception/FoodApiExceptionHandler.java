package com.foodai.backend.exception;

import com.foodai.backend.controller.AiRecognitionResultsController;
import com.foodai.backend.controller.FoodDiaryController;
import com.foodai.backend.controller.RecognitionController;
import com.foodai.backend.dto.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Higher priority than the existing BE-05 fallback advice; does not change other modules. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {RecognitionController.class, FoodDiaryController.class, AiRecognitionResultsController.class})
public class FoodApiExceptionHandler {
    @ExceptionHandler(FoodApiException.class)
    public ResponseEntity<ApiResponse<Void>> handle(FoodApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }
    // The existing project-wide fallback includes raw exception details in 500 responses.
    // Keep unexpected DB/server details out of these new APIs.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> internal(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error."));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid request body or field value."));
    }
}
