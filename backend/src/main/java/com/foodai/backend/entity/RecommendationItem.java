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
import org.hibernate.annotations.JdbcType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_items")
public class RecommendationItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "run_id", nullable = false)
	private RecommendationRun run;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "food_id")
	private Food food;

	@Column(name = "rank_position", nullable = false)
	@JdbcType(UnsignedSmallIntJdbcType.class)
	private Integer rankPosition;

	@Column(name = "relevance_score", precision = 7, scale = 5)
	private BigDecimal relevanceScore;

	@Column(name = "reason_text", length = 1000)
	private String reasonText;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected RecommendationItem() {}

	public Long getId() { return id; }
	public RecommendationRun getRun() { return run; }
	public void setRun(RecommendationRun run) { this.run = run; }
	public Food getFood() { return food; }
	public void setFood(Food food) { this.food = food; }
	public Integer getRankPosition() { return rankPosition; }
	public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }
	public BigDecimal getRelevanceScore() { return relevanceScore; }
	public void setRelevanceScore(BigDecimal relevanceScore) { this.relevanceScore = relevanceScore; }
	public String getReasonText() { return reasonText; }
	public void setReasonText(String reasonText) { this.reasonText = reasonText; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
