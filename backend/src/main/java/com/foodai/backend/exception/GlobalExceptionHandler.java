package com.foodai.backend.exception;

import com.foodai.backend.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * [BE-05] Xử lý ngoại lệ tập trung cho API upload ảnh
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidImageException(InvalidImageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Kích thước tệp vượt quá giới hạn tối đa cho phép (10MB)."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Đã xảy ra lỗi trong quá trình xử lý: " + ex.getMessage()));
    }
}
