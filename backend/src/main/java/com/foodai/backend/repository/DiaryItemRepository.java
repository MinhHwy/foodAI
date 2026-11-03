package com.foodai.backend.repository;

import com.foodai.backend.entity.DiaryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryItemRepository extends JpaRepository<DiaryItem, Long> {
    List<DiaryItem> findByMealIdInOrderByIdAsc(List<Long> mealIds);
    List<DiaryItem> findByMealIdOrderByIdAsc(Long mealId);
    Optional<DiaryItem> findByIdAndMealId(Long id, Long mealId);
    boolean existsByRecognitionItemId(Long recognitionItemId);
}
