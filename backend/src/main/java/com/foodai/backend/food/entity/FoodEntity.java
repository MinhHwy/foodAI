package com.foodai.backend.food.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mapping of the existing MySQL table `foods` (owner: food/AI catalog data).
 * Only the columns needed by the nutrition calculation APIs are mapped so that
 * insert statements leave every DEFAULT-managed column (category_id, image,
 * audit columns, data_quality, catalog_status, ...) to the database.
 *
 * Nutrition reference unit: all nutritional values are stored PER 100 GRAMS
 * (calories in kcal, protein/carbs/fat in g). standard_serving_grams is only
 * the reference portion for display and is intentionally nullable.
 */
@Entity
@Table(name = "foods")
public class FoodEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name_vi", nullable = false, length = 200)
	private String nameVi;

	@Column(name = "name_en", length = 200)
	private String nameEn;

	@Column(name = "standard_serving_label", nullable = false, length = 80)
	private String standardServingLabel;

	// References portion in grams; NULL if unknown (see table comment).
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

	public Long getId() {
		return id;
	}

	public String getNameVi() {
		return nameVi;
	}

	public void setNameVi(String nameVi) {
		this.nameVi = nameVi;
	}

	public String getNameEn() {
		return nameEn;
	}

	public void setNameEn(String nameEn) {
		this.nameEn = nameEn;
	}

	public String getStandardServingLabel() {
		return standardServingLabel;
	}

	public void setStandardServingLabel(String standardServingLabel) {
		this.standardServingLabel = standardServingLabel;
	}

	public BigDecimal getStandardServingGrams() {
		return standardServingGrams;
	}

	public void setStandardServingGrams(BigDecimal standardServingGrams) {
		this.standardServingGrams = standardServingGrams;
	}

	public BigDecimal getCaloriesPer100gKcal() {
		return caloriesPer100gKcal;
	}

	public void setCaloriesPer100gKcal(BigDecimal caloriesPer100gKcal) {
		this.caloriesPer100gKcal = caloriesPer100gKcal;
	}

	public BigDecimal getProteinPer100gG() {
		return proteinPer100gG;
	}

	public void setProteinPer100gG(BigDecimal proteinPer100gG) {
		this.proteinPer100gG = proteinPer100gG;
	}

	public BigDecimal getCarbsPer100gG() {
		return carbsPer100gG;
	}

	public void setCarbsPer100gG(BigDecimal carbsPer100gG) {
		this.carbsPer100gG = carbsPer100gG;
	}

	public BigDecimal getFatPer100gG() {
		return fatPer100gG;
	}

	public void setFatPer100gG(BigDecimal fatPer100gG) {
		this.fatPer100gG = fatPer100gG;
	}
}
