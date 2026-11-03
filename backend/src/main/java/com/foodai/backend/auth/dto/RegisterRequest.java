package com.foodai.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "Họ và tên là bắt buộc")
		@Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
		String fullName,

		@NotBlank(message = "Email là bắt buộc")
		@Email(message = "Email không đúng định dạng")
		@Size(max = 190, message = "Email tối đa 190 ký tự")
		String email,

		@NotBlank(message = "Mật khẩu là bắt buộc")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
				message = "Mật khẩu cần ít nhất 8 ký tự, bao gồm chữ cái và chữ số")
		String password) {
}
