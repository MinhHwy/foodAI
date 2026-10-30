package com.foodai.backend.exception;

/**
 * [BE-05] Ngoại lệ khi dữ liệu ảnh đầu vào không hợp lệ (sai định dạng, quá kích thước, tệp rỗng)
 */
public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }

    public InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
