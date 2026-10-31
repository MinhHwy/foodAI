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
@Table(name = "diary_items")
public class DiaryItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meal_id", nullable = false)
	private DiaryMeal meal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "food_id")
	private Food food;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recognition_item_id")
	private RecognitionItem recognitionItem;

	@Column(name = "food_name_snapshot", nullable = false, length = 200)
	private String foodNameSnapshot;

	@Column(name = "serving_label_snapshot", length = 80)
	private String servingLabelSnapshot;

	@Column(name = "portion_multiplier", nullable = false, precision = 9, scale = 3)
	private BigDecimal portionMultiplier;

	@Column(name = "consumed_grams", precision = 9, scale = 2)
	private BigDecimal consumedGrams;

	@Column(name = "calories_kcal", nullable = false, precision = 10, scale = 2)
	private BigDecimal caloriesKcal;

	@Column(name = "protein_g", nullable = false, precision = 10, scale = 2)
	private BigDecimal proteinG;

	@Column(name = "carbs_g", nullable = false, precision = 10, scale = 2)
	private BigDecimal carbsG;

	@Column(name = "fat_g", nullable = false, precision = 10, scale = 2)
	private BigDecimal fatG;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected DiaryItem() {}

	public Long getId() { return id; }
	public DiaryMeal getMeal() { return meal; }
	public void setMeal(DiaryMeal meal) { this.meal = meal; }
	public Food getFood() { return food; }
	public void setFood(Food food) { this.food = food; }
	public RecognitionItem getRecognitionItem() { return recognitionItem; }
	public void setRecognitionItem(RecognitionItem recognitionItem) { this.recognitionItem = recognitionItem; }
	public String getFoodNameSnapshot() { return foodNameSnapshot; }
	public void setFoodNameSnapshot(String foodNameSnapshot) { this.foodNameSnapshot = foodNameSnapshot; }
	public String getServingLabelSnapshot() { return servingLabelSnapshot; }
	public void setServingLabelSnapshot(String servingLabelSnapshot) { this.servingLabelSnapshot = servingLabelSnapshot; }
	public BigDecimal getPortionMultiplier() { return portionMultiplier; }
	public void setPortionMultiplier(BigDecimal portionMultiplier) { this.portionMultiplier = portionMultiplier; }
	public BigDecimal getConsumedGrams() { return consumedGrams; }
	public void setConsumedGrams(BigDecimal consumedGrams) { this.consumedGrams = consumedGrams; }
	public BigDecimal getCaloriesKcal() { return caloriesKcal; }
	public void setCaloriesKcal(BigDecimal caloriesKcal) { this.caloriesKcal = caloriesKcal; }
	public BigDecimal getProteinG() { return proteinG; }
	public void setProteinG(BigDecimal proteinG) { this.proteinG = proteinG; }
	public BigDecimal getCarbsG() { return carbsG; }
	public void setCarbsG(BigDecimal carbsG) { this.carbsG = carbsG; }
	public BigDecimal getFatG() { return fatG; }
	public void setFatG(BigDecimal fatG) { this.fatG = fatG; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
