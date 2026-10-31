package com.foodai.backend.profile.entity;

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
@Table(name = "user_settings")
public class UserSettingsEntity {

	public enum EnergyUnit {
		KCAL, KJ
	}

	public enum WeightUnit {
		KG, LB
	}

	// user_id is both the primary key and the FK to users (see schema constraint
	// fk_user_settings_user); a JPA association is unnecessary for this table.
	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "timezone_name", nullable = false, length = 64)
	private String timezoneName;

	@Enumerated(EnumType.STRING)
	@Column(name = "energy_unit", nullable = false)
	private EnergyUnit energyUnit;

	@Enumerated(EnumType.STRING)
	@Column(name = "weight_unit", nullable = false)
	private WeightUnit weightUnit;

	@Column(name = "daily_reminders_enabled", nullable = false)
	private Boolean dailyRemindersEnabled;

	@Column(name = "dietary_notes", length = 1000)
	private String dietaryNotes;

	@Column(name = "updated_at", nullable = false, columnDefinition = "datetime(3)")
	private LocalDateTime updatedAt;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getTimezoneName() {
		return timezoneName;
	}

	public void setTimezoneName(String timezoneName) {
		this.timezoneName = timezoneName;
	}

	public EnergyUnit getEnergyUnit() {
		return energyUnit;
	}

	public void setEnergyUnit(EnergyUnit energyUnit) {
		this.energyUnit = energyUnit;
	}

	public WeightUnit getWeightUnit() {
		return weightUnit;
	}

	public void setWeightUnit(WeightUnit weightUnit) {
		this.weightUnit = weightUnit;
	}

	public Boolean getDailyRemindersEnabled() {
		return dailyRemindersEnabled;
	}

	public void setDailyRemindersEnabled(Boolean dailyRemindersEnabled) {
		this.dailyRemindersEnabled = dailyRemindersEnabled;
	}

	public String getDietaryNotes() {
		return dietaryNotes;
	}

	public void setDietaryNotes(String dietaryNotes) {
		this.dietaryNotes = dietaryNotes;
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
