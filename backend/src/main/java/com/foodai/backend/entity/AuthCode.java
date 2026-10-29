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
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_codes")
public class AuthCode {
	public enum Purpose { VERIFY_EMAIL, RESET_PASSWORD }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "purpose", nullable = false)
	private Purpose purpose;

	@Column(name = "attempts", nullable = false)
	private Short attempts;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "consumed_at")
	private LocalDateTime consumedAt;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected AuthCode() {}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public String getCodeHash() { return codeHash; }
	public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
	public Purpose getPurpose() { return purpose; }
	public void setPurpose(Purpose purpose) { this.purpose = purpose; }
	public Short getAttempts() { return attempts; }
	public void setAttempts(Short attempts) { this.attempts = attempts; }
	public LocalDateTime getExpiresAt() { return expiresAt; }
	public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
	public LocalDateTime getConsumedAt() { return consumedAt; }
	public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
