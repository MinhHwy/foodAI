package com.foodai.backend.repository;

import com.foodai.backend.entity.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {}
