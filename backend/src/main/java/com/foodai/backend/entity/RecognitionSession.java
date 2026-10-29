package com.foodai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "recognition_sessions")
public class RecognitionSession {
	public enum Status { UPLOADED, PROCESSING, COMPLETED, REJECTED, FAILED }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "image_url", nullable = false, length = 1024)
	private String imageUrl;

	@Column(name = "cloudinary_public_id", length = 255)
	private String cloudinaryPublicId;

	@Column(name = "mime_type", length = 100)
	private String mimeType;

	@Column(name = "file_size_bytes")
	private Long fileSizeBytes;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private Status status;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	@Column(name = "model_version", length = 100)
	private String modelVersion;

	@Column(name = "error_code", length = 100)
	private String errorCode;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	protected RecognitionSession() {}

	public Long getId() { return id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public String getImageUrl() { return imageUrl; }
	public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
	public String getCloudinaryPublicId() { return cloudinaryPublicId; }
	public void setCloudinaryPublicId(String cloudinaryPublicId) { this.cloudinaryPublicId = cloudinaryPublicId; }
	public String getMimeType() { return mimeType; }
	public void setMimeType(String mimeType) { this.mimeType = mimeType; }
	public Long getFileSizeBytes() { return fileSizeBytes; }
	public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
	public Status getStatus() { return status; }
	public void setStatus(Status status) { this.status = status; }
	public String getRejectionReason() { return rejectionReason; }
	public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
	public String getModelVersion() { return modelVersion; }
	public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
	public String getErrorCode() { return errorCode; }
	public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getFinishedAt() { return finishedAt; }
	public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
