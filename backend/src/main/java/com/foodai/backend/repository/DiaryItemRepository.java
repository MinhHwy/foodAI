package com.foodai.backend.repository;

import com.foodai.backend.entity.DiaryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryItemRepository extends JpaRepository<DiaryItem, Long> {}
