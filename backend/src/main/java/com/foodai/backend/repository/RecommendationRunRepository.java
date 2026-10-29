package com.foodai.backend.repository;

import com.foodai.backend.entity.RecommendationRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRunRepository extends JpaRepository<RecommendationRun, Long> {}
