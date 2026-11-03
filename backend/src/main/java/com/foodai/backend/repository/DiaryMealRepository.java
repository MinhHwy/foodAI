package com.foodai.backend.repository;

import com.foodai.backend.entity.DiaryMeal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryMealRepository extends JpaRepository<DiaryMeal, Long> {
    Optional<DiaryMeal> findByIdAndUserId(Long id, Long userId);
    List<DiaryMeal> findByUserIdAndLocalMealDateBetweenOrderByEatenAtUtcDesc(
            Long userId, LocalDate from, LocalDate to);
}
