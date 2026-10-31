package com.foodai.backend.food.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodai.backend.food.entity.FoodEntity;

public interface FoodRepository extends JpaRepository<FoodEntity, Long> {
}
