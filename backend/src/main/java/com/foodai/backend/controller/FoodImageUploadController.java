package com.foodai.backend.controller;

import com.foodai.backend.dto.response.ApiResponse;
import com.foodai.backend.dto.response.ImageUploadResponse;
import com.foodai.backend.service.CloudinaryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * [BE-05] REST Controller cung cấp API tải ảnh thực phẩm lên Cloudinary
 */
@RestController
@RequestMapping("/api/v1/images")
@CrossOrigin(origins = "*") // Hỗ trợ frontend gọi API trong giai đoạn phát triển
public class FoodImageUploadController {

    private final CloudinaryService cloudinaryService;

    public FoodImageUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * API tải lên ảnh món ăn
     * Endpoint: POST /api/v1/images/upload
     * Content-Type: multipart/form-data
     *
     * @param file Tệp ảnh món ăn do người dùng gửi
     * @return Phản hồi chứa URL Cloudinary và thông tin ảnh
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadFoodImage(
            @RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = cloudinaryService.uploadFoodImage(file);
        return ResponseEntity.ok(ApiResponse.success("Tải ảnh món ăn lên thành công.", response));
    }

    /**
     * API xóa ảnh trên Cloudinary theo publicId
     * Endpoint: DELETE /api/v1/images/{publicId}
     *
     * @param publicId Public ID của ảnh trên Cloudinary
     */
    @DeleteMapping("/{publicId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteFoodImage(
            @PathVariable("publicId") String publicId) {
        Map<String, Object> result = cloudinaryService.deleteImage(publicId);
        return ResponseEntity.ok(ApiResponse.success("Xóa ảnh thành công.", result));
    }

    /**
     * Endpoint kiểm tra trạng thái hoạt động của Image Service
     * Endpoint: GET /api/v1/images/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> checkHealth() {
        return ResponseEntity.ok(ApiResponse.success("Food Image Upload Service is running normally.", "READY"));
    }
}
