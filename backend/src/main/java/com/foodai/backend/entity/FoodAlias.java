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
@Table(name = "food_aliases")
public class FoodAlias {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "food_id", nullable = false)
	private Food food;

	@Column(name = "alias_name", nullable = false, length = 200)
	private String aliasName;

	@Column(name = "language_tag", nullable = false, length = 12)
	private String languageTag;

	protected FoodAlias() {}

	public Long getId() { return id; }
	public Food getFood() { return food; }
	public void setFood(Food food) { this.food = food; }
	public String getAliasName() { return aliasName; }
	public void setAliasName(String aliasName) { this.aliasName = aliasName; }
	public String getLanguageTag() { return languageTag; }
	public void setLanguageTag(String languageTag) { this.languageTag = languageTag; }
}
