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
@Table(name = "weight_logs")
public class WeightLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "weight_kg", nullable = false, precision = 7, scale = 2)
	private BigDecimal weightKg;

	@Column(name = "measured_at_utc", nullable = false)
	private LocalDateTime measuredAtUtc;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected WeightLog() {}

	public Long getId() { return id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public BigDecimal getWeightKg() { return weightKg; }
	public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
	public LocalDateTime getMeasuredAtUtc() { return measuredAtUtc; }
	public void setMeasuredAtUtc(LocalDateTime measuredAtUtc) { this.measuredAtUtc = measuredAtUtc; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
