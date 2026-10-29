package com.foodai.backend.repository;

import com.foodai.backend.entity.RecognitionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecognitionItemRepository extends JpaRepository<RecognitionItem, Long> {}
