package com.foodai.backend.profile.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.foodai.backend.common.time.UtcTimes;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {

	public enum BiologicalSex {
		MALE, FEMALE, UNSPECIFIED
	}

	public enum ActivityLevel {
		SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE
	}

	// user_id is both the primary key and the FK to users (see schema constraint
	// fk_user_profiles_user); a JPA association is unnecessary for this table.
	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "age_years", columnDefinition = "smallint unsigned")
	private Integer ageYears;

	@Enumerated(EnumType.STRING)
	@Column(name = "biological_sex", nullable = false)
	private BiologicalSex biologicalSex;

	@Column(name = "height_cm", precision = 6, scale = 2)
	private BigDecimal heightCm;

	@Column(name = "current_weight_kg", precision = 7, scale = 2)
	private BigDecimal currentWeightKg;

	@Enumerated(EnumType.STRING)
	@Column(name = "activity_level")
	private ActivityLevel activityLevel;

	@Column(name = "avatar_url", length = 1024)
	private String avatarUrl;

	@Column(name = "updated_at", nullable = false, columnDefinition = "datetime(3)")
	private LocalDateTime updatedAt;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Integer getAgeYears() {
		return ageYears;
	}

	public void setAgeYears(Integer ageYears) {
		this.ageYears = ageYears;
	}

	public BiologicalSex getBiologicalSex() {
		return biologicalSex;
	}

	public void setBiologicalSex(BiologicalSex biologicalSex) {
		this.biologicalSex = biologicalSex;
	}

	public BigDecimal getHeightCm() {
		return heightCm;
	}

	public void setHeightCm(BigDecimal heightCm) {
		this.heightCm = heightCm;
	}

	public BigDecimal getCurrentWeightKg() {
		return currentWeightKg;
	}

	public void setCurrentWeightKg(BigDecimal currentWeightKg) {
		this.currentWeightKg = currentWeightKg;
	}

	public ActivityLevel getActivityLevel() {
		return activityLevel;
	}

	public void setActivityLevel(ActivityLevel activityLevel) {
		this.activityLevel = activityLevel;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	@PrePersist
	void onPrePersist() {
		if (updatedAt == null) {
			updatedAt = UtcTimes.nowUtc();
		}
	}

	@PreUpdate
	void onPreUpdate() {
		updatedAt = UtcTimes.nowUtc();
	}
}
