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

@Entity
@Table(name = "model_food_classes")
public class ModelFoodClass {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "model_version", nullable = false, length = 100)
	private String modelVersion;

	@Column(name = "class_label", nullable = false, length = 200)
	private String classLabel;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "food_id", nullable = false)
	private Food food;

	protected ModelFoodClass() {}

	public Long getId() { return id; }
	public String getModelVersion() { return modelVersion; }
	public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
	public String getClassLabel() { return classLabel; }
	public void setClassLabel(String classLabel) { this.classLabel = classLabel; }
	public Food getFood() { return food; }
	public void setFood(Food food) { this.food = food; }
}
