package com.foodai.backend.service;

import com.foodai.backend.dto.response.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * [BE-05] Interface dịch vụ tải và quản lý ảnh với Cloudinary
 */
public interface CloudinaryService {

    /**
     * Tải ảnh món ăn lên Cloudinary kèm kiểm tra tính hợp lệ dữ liệu đầu vào.
     *
     * @param file Tệp ảnh do người dùng gửi
     * @return Thông tin ảnh sau khi tải lên thành công
     */
    ImageUploadResponse uploadFoodImage(MultipartFile file);

    /**
     * Xóa ảnh trên Cloudinary theo publicId
     *
     * @param publicId Định danh ảnh trên Cloudinary
     * @return Kết quả xóa
     */
    Map<String, Object> deleteImage(String publicId);
}
