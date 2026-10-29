package com.foodai.backend.repository;

import com.foodai.backend.entity.RecognitionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecognitionFeedbackRepository extends JpaRepository<RecognitionFeedback, Long> {}
