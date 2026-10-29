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
import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
public class UserSettings {
	public enum EnergyUnit { KCAL, KJ }
	public enum WeightUnit { KG, LB }

	@Id
	@Column(name = "user_id")
	private Long userId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

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

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected UserSettings() {}

	public Long getUserId() { return userId; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public String getTimezoneName() { return timezoneName; }
	public void setTimezoneName(String timezoneName) { this.timezoneName = timezoneName; }
	public EnergyUnit getEnergyUnit() { return energyUnit; }
	public void setEnergyUnit(EnergyUnit energyUnit) { this.energyUnit = energyUnit; }
	public WeightUnit getWeightUnit() { return weightUnit; }
	public void setWeightUnit(WeightUnit weightUnit) { this.weightUnit = weightUnit; }
	public Boolean getDailyRemindersEnabled() { return dailyRemindersEnabled; }
	public void setDailyRemindersEnabled(Boolean dailyRemindersEnabled) { this.dailyRemindersEnabled = dailyRemindersEnabled; }
	public String getDietaryNotes() { return dietaryNotes; }
	public void setDietaryNotes(String dietaryNotes) { this.dietaryNotes = dietaryNotes; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
