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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "diary_meals")
public class DiaryMeal {
	public enum SourceType { MANUAL, RECOGNITION, RECOMMENDATION }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "meal_type", nullable = false)
	private MealType mealType;

	@Column(name = "eaten_at_utc", nullable = false)
	private LocalDateTime eatenAtUtc;

	@Column(name = "local_meal_date", nullable = false)
	private LocalDate localMealDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false)
	private SourceType sourceType;

	@Column(name = "notes", length = 500)
	private String notes;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected DiaryMeal() {}

	public Long getId() { return id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public MealType getMealType() { return mealType; }
	public void setMealType(MealType mealType) { this.mealType = mealType; }
	public LocalDateTime getEatenAtUtc() { return eatenAtUtc; }
	public void setEatenAtUtc(LocalDateTime eatenAtUtc) { this.eatenAtUtc = eatenAtUtc; }
	public LocalDate getLocalMealDate() { return localMealDate; }
	public void setLocalMealDate(LocalDate localMealDate) { this.localMealDate = localMealDate; }
	public SourceType getSourceType() { return sourceType; }
	public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
	public String getNotes() { return notes; }
	public void setNotes(String notes) { this.notes = notes; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
