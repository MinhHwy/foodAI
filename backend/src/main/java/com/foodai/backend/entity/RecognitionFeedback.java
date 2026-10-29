package com.foodai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recognition_feedback")
public class RecognitionFeedback {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recognition_item_id", nullable = false)
	private RecognitionItem recognitionItem;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "previous_food_id")
	private Food previousFood;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "corrected_food_id")
	private Food correctedFood;

	@Column(name = "previous_portion_multiplier", precision = 9, scale = 3)
	private BigDecimal previousPortionMultiplier;

	@Column(name = "corrected_portion_multiplier", precision = 9, scale = 3)
	private BigDecimal correctedPortionMultiplier;

	@Column(name = "correction_note", length = 500)
	private String correctionNote;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected RecognitionFeedback() {}

	public Long getId() { return id; }
	public RecognitionItem getRecognitionItem() { return recognitionItem; }
	public void setRecognitionItem(RecognitionItem recognitionItem) { this.recognitionItem = recognitionItem; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public Food getPreviousFood() { return previousFood; }
	public void setPreviousFood(Food previousFood) { this.previousFood = previousFood; }
	public Food getCorrectedFood() { return correctedFood; }
	public void setCorrectedFood(Food correctedFood) { this.correctedFood = correctedFood; }
	public BigDecimal getPreviousPortionMultiplier() { return previousPortionMultiplier; }
	public void setPreviousPortionMultiplier(BigDecimal previousPortionMultiplier) { this.previousPortionMultiplier = previousPortionMultiplier; }
	public BigDecimal getCorrectedPortionMultiplier() { return correctedPortionMultiplier; }
	public void setCorrectedPortionMultiplier(BigDecimal correctedPortionMultiplier) { this.correctedPortionMultiplier = correctedPortionMultiplier; }
	public String getCorrectionNote() { return correctionNote; }
	public void setCorrectionNote(String correctionNote) { this.correctionNote = correctionNote; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
