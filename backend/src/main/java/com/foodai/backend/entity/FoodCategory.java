package com.foodai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "food_categories")
public class FoodCategory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "name_vi", nullable = false, length = 100)
	private String nameVi;

	@Column(name = "name_en", length = 100)
	private String nameEn;

	protected FoodCategory() {}

	public Long getId() { return id; }
	public String getNameVi() { return nameVi; }
	public void setNameVi(String nameVi) { this.nameVi = nameVi; }
	public String getNameEn() { return nameEn; }
	public void setNameEn(String nameEn) { this.nameEn = nameEn; }
}
