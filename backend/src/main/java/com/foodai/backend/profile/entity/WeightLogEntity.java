package com.foodai.backend.profile.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.foodai.backend.common.time.UtcTimes;

@Entity
@Table(name = "weight_logs")
public class WeightLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "weight_kg", nullable = false, precision = 7, scale = 2)
	private BigDecimal weightKg;

	@Column(name = "measured_at_utc", nullable = false, columnDefinition = "datetime(3)")
	private LocalDateTime measuredAtUtc;

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

	public BigDecimal getWeightKg() {
		return weightKg;
	}

	public void setWeightKg(BigDecimal weightKg) {
		this.weightKg = weightKg;
	}

	public LocalDateTime getMeasuredAtUtc() {
		return measuredAtUtc;
	}

	public void setMeasuredAtUtc(LocalDateTime measuredAtUtc) {
		this.measuredAtUtc = measuredAtUtc;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	@PrePersist
	void onPrePersist() {
		if (measuredAtUtc == null) {
			measuredAtUtc = UtcTimes.nowUtc();
		}
		if (createdAt == null) {
			createdAt = UtcTimes.nowUtc();
		}
	}
}
