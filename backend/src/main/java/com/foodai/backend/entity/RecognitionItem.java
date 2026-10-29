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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "recognition_items")
public class RecognitionItem {
	public enum PortionMethod { AI_ESTIMATE, REFERENCE_OBJECT, MANUAL, UNKNOWN }
	public enum ItemStatus { PREDICTED, CONFIRMED, REJECTED }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private RecognitionSession session;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "predicted_food_id")
	private Food predictedFood;

	@Column(name = "predicted_label", length = 200)
	private String predictedLabel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "selected_food_id")
	private Food selectedFood;

	@Column(name = "confidence_score", precision = 6, scale = 5)
	private BigDecimal confidenceScore;

	@Column(name = "bbox_x", precision = 7, scale = 6)
	private BigDecimal bboxX;

	@Column(name = "bbox_y", precision = 7, scale = 6)
	private BigDecimal bboxY;

	@Column(name = "bbox_width", precision = 7, scale = 6)
	private BigDecimal bboxWidth;

	@Column(name = "bbox_height", precision = 7, scale = 6)
	private BigDecimal bboxHeight;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "detection_metadata", columnDefinition = "JSON")
	private Map<String, Object> detectionMetadata;

	@Column(name = "estimated_grams", precision = 9, scale = 2)
	private BigDecimal estimatedGrams;

	@Column(name = "portion_multiplier", precision = 9, scale = 3)
	private BigDecimal portionMultiplier;

	@Enumerated(EnumType.STRING)
	@Column(name = "portion_method", nullable = false)
	private PortionMethod portionMethod;

	@Column(name = "portion_confidence", precision = 6, scale = 5)
	private BigDecimal portionConfidence;

	@Column(name = "explanation", length = 1000)
	private String explanation;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_status", nullable = false)
	private ItemStatus itemStatus;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected RecognitionItem() {}

	public Long getId() { return id; }
	public RecognitionSession getSession() { return session; }
	public void setSession(RecognitionSession session) { this.session = session; }
	public Food getPredictedFood() { return predictedFood; }
	public void setPredictedFood(Food predictedFood) { this.predictedFood = predictedFood; }
	public String getPredictedLabel() { return predictedLabel; }
	public void setPredictedLabel(String predictedLabel) { this.predictedLabel = predictedLabel; }
	public Food getSelectedFood() { return selectedFood; }
	public void setSelectedFood(Food selectedFood) { this.selectedFood = selectedFood; }
	public BigDecimal getConfidenceScore() { return confidenceScore; }
	public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
	public BigDecimal getBboxX() { return bboxX; }
	public void setBboxX(BigDecimal bboxX) { this.bboxX = bboxX; }
	public BigDecimal getBboxY() { return bboxY; }
	public void setBboxY(BigDecimal bboxY) { this.bboxY = bboxY; }
	public BigDecimal getBboxWidth() { return bboxWidth; }
	public void setBboxWidth(BigDecimal bboxWidth) { this.bboxWidth = bboxWidth; }
	public BigDecimal getBboxHeight() { return bboxHeight; }
	public void setBboxHeight(BigDecimal bboxHeight) { this.bboxHeight = bboxHeight; }
	public Map<String, Object> getDetectionMetadata() { return detectionMetadata; }
	public void setDetectionMetadata(Map<String, Object> detectionMetadata) { this.detectionMetadata = detectionMetadata; }
	public BigDecimal getEstimatedGrams() { return estimatedGrams; }
	public void setEstimatedGrams(BigDecimal estimatedGrams) { this.estimatedGrams = estimatedGrams; }
	public BigDecimal getPortionMultiplier() { return portionMultiplier; }
	public void setPortionMultiplier(BigDecimal portionMultiplier) { this.portionMultiplier = portionMultiplier; }
	public PortionMethod getPortionMethod() { return portionMethod; }
	public void setPortionMethod(PortionMethod portionMethod) { this.portionMethod = portionMethod; }
	public BigDecimal getPortionConfidence() { return portionConfidence; }
	public void setPortionConfidence(BigDecimal portionConfidence) { this.portionConfidence = portionConfidence; }
	public String getExplanation() { return explanation; }
	public void setExplanation(String explanation) { this.explanation = explanation; }
	public ItemStatus getItemStatus() { return itemStatus; }
	public void setItemStatus(ItemStatus itemStatus) { this.itemStatus = itemStatus; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
