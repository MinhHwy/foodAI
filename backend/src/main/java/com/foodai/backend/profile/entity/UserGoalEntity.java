package com.foodai.backend.profile.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.foodai.backend.common.time.UtcTimes;

@Entity
@Table(name = "user_goals")
public class UserGoalEntity {

	public enum GoalType {
		LOSE, MAINTAIN, GAIN
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "goal_type", nullable = false)
	private GoalType goalType;

	@Column(name = "target_weight_kg", precision = 7, scale = 2)
	private BigDecimal targetWeightKg;

	@Column(name = "daily_calories_kcal", nullable = false, precision = 9, scale = 2)
	private BigDecimal dailyCaloriesKcal;

	@Column(name = "protein_target_g", nullable = false, precision = 9, scale = 2)
	private BigDecimal proteinTargetG;

	@Column(name = "carbs_target_g", nullable = false, precision = 9, scale = 2)
	private BigDecimal carbsTargetG;

	@Column(name = "fat_target_g", nullable = false, precision = 9, scale = 2)
	private BigDecimal fatTargetG;

	@Column(name = "estimated_bmr_kcal", precision = 9, scale = 2)
	private BigDecimal estimatedBmrKcal;

	@Column(name = "estimated_tdee_kcal", precision = 9, scale = 2)
	private BigDecimal estimatedTdeeKcal;

	@Column(name = "calculation_method", length = 80)
	private String calculationMethod;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "effective_to")
	private LocalDate effectiveTo;

	@Column(name = "created_at", nullable = false, columnDefinition = "datetime(3)", updatable = false)
	private LocalDateTime createdAt;

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public GoalType getGoalType() {
		return goalType;
	}

	public void setGoalType(GoalType goalType) {
		this.goalType = goalType;
	}

	public BigDecimal getTargetWeightKg() {
		return targetWeightKg;
	}

	public void setTargetWeightKg(BigDecimal targetWeightKg) {
		this.targetWeightKg = targetWeightKg;
	}

	public BigDecimal getDailyCaloriesKcal() {
		return dailyCaloriesKcal;
	}

	public void setDailyCaloriesKcal(BigDecimal dailyCaloriesKcal) {
		this.dailyCaloriesKcal = dailyCaloriesKcal;
	}

	public BigDecimal getProteinTargetG() {
		return proteinTargetG;
	}

	public void setProteinTargetG(BigDecimal proteinTargetG) {
		this.proteinTargetG = proteinTargetG;
	}

	public BigDecimal getCarbsTargetG() {
		return carbsTargetG;
	}

	public void setCarbsTargetG(BigDecimal carbsTargetG) {
		this.carbsTargetG = carbsTargetG;
	}

	public BigDecimal getFatTargetG() {
		return fatTargetG;
	}

	public void setFatTargetG(BigDecimal fatTargetG) {
		this.fatTargetG = fatTargetG;
	}

	public BigDecimal getEstimatedBmrKcal() {
		return estimatedBmrKcal;
	}

	public void setEstimatedBmrKcal(BigDecimal estimatedBmrKcal) {
		this.estimatedBmrKcal = estimatedBmrKcal;
	}

	public BigDecimal getEstimatedTdeeKcal() {
		return estimatedTdeeKcal;
	}

	public void setEstimatedTdeeKcal(BigDecimal estimatedTdeeKcal) {
		this.estimatedTdeeKcal = estimatedTdeeKcal;
	}

	public String getCalculationMethod() {
		return calculationMethod;
	}

	public void setCalculationMethod(String calculationMethod) {
		this.calculationMethod = calculationMethod;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(LocalDate effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	public LocalDate getEffectiveTo() {
		return effectiveTo;
	}

	public void setEffectiveTo(LocalDate effectiveTo) {
		this.effectiveTo = effectiveTo;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	@PrePersist
	void onPrePersist() {
		if (createdAt == null) {
			createdAt = UtcTimes.nowUtc();
		}
	}
}
