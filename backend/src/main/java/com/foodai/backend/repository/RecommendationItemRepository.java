package com.foodai.backend.repository;

import com.foodai.backend.entity.RecommendationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {}
