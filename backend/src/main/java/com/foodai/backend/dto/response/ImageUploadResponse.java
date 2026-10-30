package com.foodai.backend.dto.response;

/**
 * [BE-05] DTO trả về sau khi tải ảnh thực phẩm lên Cloudinary thành công
 */
public class ImageUploadResponse {

    private String publicId;
    private String secureUrl;
    private String originalFilename;
    private String format;
    private long sizeInBytes;
    private int width;
    private int height;

    public ImageUploadResponse() {
    }

    public ImageUploadResponse(String publicId, String secureUrl, String originalFilename,
                               String format, long sizeInBytes, int width, int height) {
        this.publicId = publicId;
        this.secureUrl = secureUrl;
        this.originalFilename = originalFilename;
        this.format = format;
        this.sizeInBytes = sizeInBytes;
        this.width = width;
        this.height = height;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getSecureUrl() {
        return secureUrl;
    }

    public void setSecureUrl(String secureUrl) {
        this.secureUrl = secureUrl;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
