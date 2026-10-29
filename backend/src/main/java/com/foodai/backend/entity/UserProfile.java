package com.foodai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
	public enum BiologicalSex { MALE, FEMALE, UNSPECIFIED }
	public enum ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }

	@Id
	@Column(name = "user_id")
	private Long userId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "age_years")
	private Short ageYears;

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

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected UserProfile() {}

	public Long getUserId() { return userId; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public Short getAgeYears() { return ageYears; }
	public void setAgeYears(Short ageYears) { this.ageYears = ageYears; }
	public BiologicalSex getBiologicalSex() { return biologicalSex; }
	public void setBiologicalSex(BiologicalSex biologicalSex) { this.biologicalSex = biologicalSex; }
	public BigDecimal getHeightCm() { return heightCm; }
	public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
	public BigDecimal getCurrentWeightKg() { return currentWeightKg; }
	public void setCurrentWeightKg(BigDecimal currentWeightKg) { this.currentWeightKg = currentWeightKg; }
	public ActivityLevel getActivityLevel() { return activityLevel; }
	public void setActivityLevel(ActivityLevel activityLevel) { this.activityLevel = activityLevel; }
	public String getAvatarUrl() { return avatarUrl; }
	public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
