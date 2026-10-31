package com.foodai.backend.common.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemError> handleApiException(ApiException ex) {
		return ProblemError.of(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemError> handleValidation(MethodArgumentNotValidException ex) {
		StringBuilder detail = new StringBuilder("Dữ liệu không hợp lệ");
		return ProblemError.of(HttpStatus.BAD_REQUEST, detail.toString(),
				ex.getBindingResult().getFieldErrors().stream()
						.map(error -> error.getField() + ": " + error.getDefaultMessage())
						.toList());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ProblemError> handleUnreadable(HttpMessageNotReadableException ex) {
		return ProblemError.of(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không đọc được hoặc thiếu.");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return ProblemError.of(HttpStatus.BAD_REQUEST, "Kiểu dữ liệu không hợp lệ cho trường '" + ex.getName() + "'.");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemError> handleIntegrity(DataIntegrityViolationException ex) {
		return ProblemError.of(HttpStatus.CONFLICT, "Dữ liệu vi phạm ràng buộc (có thể email đã tồn tại).");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemError> handleGeneric(Exception ex) {
		return ProblemError.of(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống. Vui lòng thử lại.");
	}

	public record ProblemError(String type, String title, int status, String detail, java.util.List<String> fieldErrors) {

		public static ResponseEntity<ProblemError> of(HttpStatus status, String detail) {
			return of(status, detail, null);
		}

		public static ResponseEntity<ProblemError> of(HttpStatus status, String detail, java.util.List<String> fieldErrors) {
			ProblemError body = new ProblemError("about:blank", status.getReasonPhrase(), status.value(), detail, fieldErrors);
			return ResponseEntity.status(status).body(body);
		}
	}
}
