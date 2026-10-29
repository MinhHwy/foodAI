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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "foods")
public class Food {
	public enum DataQuality { DEMO_UNVERIFIED, ESTIMATED, VERIFIED }
	public enum CatalogStatus { DRAFT, PUBLISHED, ARCHIVED }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private FoodCategory category;

	@Column(name = "name_vi", nullable = false, length = 200)
	private String nameVi;

	@Column(name = "name_en", length = 200)
	private String nameEn;

	@Column(name = "region_vi", length = 120)
	private String regionVi;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "ingredients_summary", columnDefinition = "TEXT")
	private String ingredientsSummary;

	@Column(name = "image_url", length = 1024)
	private String imageUrl;

	@Column(name = "standard_serving_label", nullable = false, length = 80)
	private String standardServingLabel;

	@Column(name = "standard_serving_grams", precision = 9, scale = 2)
	private BigDecimal standardServingGrams;

	@Column(name = "calories_per_100g_kcal", nullable = false, precision = 8, scale = 2)
	private BigDecimal caloriesPer100gKcal;

	@Column(name = "protein_per_100g_g", nullable = false, precision = 8, scale = 2)
	private BigDecimal proteinPer100gG;

	@Column(name = "carbs_per_100g_g", nullable = false, precision = 8, scale = 2)
	private BigDecimal carbsPer100gG;

	@Column(name = "fat_per_100g_g", nullable = false, precision = 8, scale = 2)
	private BigDecimal fatPer100gG;

	@Enumerated(EnumType.STRING)
	@Column(name = "data_quality", nullable = false)
	private DataQuality dataQuality;

	@Enumerated(EnumType.STRING)
	@Column(name = "catalog_status", nullable = false)
	private CatalogStatus catalogStatus;

	@Column(name = "nutrition_source", length = 500)
	private String nutritionSource;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_by")
	private User updatedBy;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected Food() {}

	public Long getId() { return id; }
	public FoodCategory getCategory() { return category; }
	public void setCategory(FoodCategory category) { this.category = category; }
	public String getNameVi() { return nameVi; }
	public void setNameVi(String nameVi) { this.nameVi = nameVi; }
	public String getNameEn() { return nameEn; }
	public void setNameEn(String nameEn) { this.nameEn = nameEn; }
	public String getRegionVi() { return regionVi; }
	public void setRegionVi(String regionVi) { this.regionVi = regionVi; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getIngredientsSummary() { return ingredientsSummary; }
	public void setIngredientsSummary(String ingredientsSummary) { this.ingredientsSummary = ingredientsSummary; }
	public String getImageUrl() { return imageUrl; }
	public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
	public String getStandardServingLabel() { return standardServingLabel; }
	public void setStandardServingLabel(String standardServingLabel) { this.standardServingLabel = standardServingLabel; }
	public BigDecimal getStandardServingGrams() { return standardServingGrams; }
	public void setStandardServingGrams(BigDecimal standardServingGrams) { this.standardServingGrams = standardServingGrams; }
	public BigDecimal getCaloriesPer100gKcal() { return caloriesPer100gKcal; }
	public void setCaloriesPer100gKcal(BigDecimal caloriesPer100gKcal) { this.caloriesPer100gKcal = caloriesPer100gKcal; }
	public BigDecimal getProteinPer100gG() { return proteinPer100gG; }
	public void setProteinPer100gG(BigDecimal proteinPer100gG) { this.proteinPer100gG = proteinPer100gG; }
	public BigDecimal getCarbsPer100gG() { return carbsPer100gG; }
	public void setCarbsPer100gG(BigDecimal carbsPer100gG) { this.carbsPer100gG = carbsPer100gG; }
	public BigDecimal getFatPer100gG() { return fatPer100gG; }
	public void setFatPer100gG(BigDecimal fatPer100gG) { this.fatPer100gG = fatPer100gG; }
	public DataQuality getDataQuality() { return dataQuality; }
	public void setDataQuality(DataQuality dataQuality) { this.dataQuality = dataQuality; }
	public CatalogStatus getCatalogStatus() { return catalogStatus; }
	public void setCatalogStatus(CatalogStatus catalogStatus) { this.catalogStatus = catalogStatus; }
	public String getNutritionSource() { return nutritionSource; }
	public void setNutritionSource(String nutritionSource) { this.nutritionSource = nutritionSource; }
	public User getCreatedBy() { return createdBy; }
	public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
	public User getUpdatedBy() { return updatedBy; }
	public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
