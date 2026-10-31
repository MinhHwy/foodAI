package com.foodai.backend.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodai.backend.profile.entity.WeightLogEntity;

public interface WeightLogRepository extends JpaRepository<WeightLogEntity, Long> {
}
