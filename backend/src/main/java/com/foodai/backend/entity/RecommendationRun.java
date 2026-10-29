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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_runs")
public class RecommendationRun {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "goal_id")
	private UserGoal goal;

	@Column(name = "target_date", nullable = false)
	private LocalDate targetDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_meal_type")
	private MealType targetMealType;

	@Column(name = "remaining_calories_kcal", precision = 10, scale = 2)
	private BigDecimal remainingCaloriesKcal;

	@Column(name = "algorithm_version", length = 80)
	private String algorithmVersion;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected RecommendationRun() {}

	public Long getId() { return id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public UserGoal getGoal() { return goal; }
	public void setGoal(UserGoal goal) { this.goal = goal; }
	public LocalDate getTargetDate() { return targetDate; }
	public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
	public MealType getTargetMealType() { return targetMealType; }
	public void setTargetMealType(MealType targetMealType) { this.targetMealType = targetMealType; }
	public BigDecimal getRemainingCaloriesKcal() { return remainingCaloriesKcal; }
	public void setRemainingCaloriesKcal(BigDecimal remainingCaloriesKcal) { this.remainingCaloriesKcal = remainingCaloriesKcal; }
	public String getAlgorithmVersion() { return algorithmVersion; }
	public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
