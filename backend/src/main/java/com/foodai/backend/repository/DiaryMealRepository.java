package com.foodai.backend.repository;

import com.foodai.backend.entity.DiaryMeal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryMealRepository extends JpaRepository<DiaryMeal, Long> {}
