package com.foodai.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.foodai.backend.dto.response.ImageUploadResponse;
import com.foodai.backend.exception.InvalidImageException;
import com.foodai.backend.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * [BE-05] Implementation tích hợp Cloudinary và kiểm tra an toàn dữ liệu đầu vào (FR04, NFR01)
 */
@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:foodai/food_images}")
    private String uploadFolder;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public ImageUploadResponse uploadFoodImage(MultipartFile file) {
        // 1. Kiểm tra file rỗng
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Vui lòng chọn một tệp hình ảnh món ăn hợp lệ.");
        }

        // 2. Kiểm tra dung lượng file (tối đa 10MB)
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidImageException("Kích thước tệp vượt quá giới hạn 10MB cho phép.");
        }

        // 3. Kiểm tra MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidImageException("Định dạng ảnh không được hỗ trợ. Chỉ chấp nhận các định dạng: JPG, JPEG, PNG, WEBP.");
        }

        // 4. Kiểm tra đuôi mở rộng file gốc
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidExtension(originalFilename)) {
            throw new InvalidImageException("Phần mở rộng tệp không hợp lệ. Vui lòng tải lên tệp ảnh hợp lệ.");
        }

        try {
            // 5. Đặt tên file an toàn để tránh ghi đè và tấn công Path Traversal
            String customPublicId = "food_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", uploadFolder,
                    "public_id", customPublicId,
                    "resource_type", "image",
                    "overwrite", false
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            String publicId = (String) uploadResult.get("public_id");
            String secureUrl = (String) uploadResult.get("secure_url");
            String format = (String) uploadResult.get("format");
            Number bytesNumber = (Number) uploadResult.get("bytes");
            Number widthNumber = (Number) uploadResult.get("width");
            Number heightNumber = (Number) uploadResult.get("height");

            long bytes = bytesNumber != null ? bytesNumber.longValue() : file.getSize();
            int width = widthNumber != null ? widthNumber.intValue() : 0;
            int height = heightNumber != null ? heightNumber.intValue() : 0;

            return new ImageUploadResponse(
                    publicId,
                    secureUrl,
                    originalFilename,
                    format,
                    bytes,
                    width,
                    height
            );

        } catch (IOException e) {
            throw new InvalidImageException("Tải ảnh lên dịch vụ Cloudinary thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> deleteImage(String publicId) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new InvalidImageException("Public ID của ảnh không được để trống.");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return result;
        } catch (IOException e) {
            throw new InvalidImageException("Xóa ảnh trên Cloudinary thất bại: " + e.getMessage(), e);
        }
    }

    private boolean hasValidExtension(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }
}
