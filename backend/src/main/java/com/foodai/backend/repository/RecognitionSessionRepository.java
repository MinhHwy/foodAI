package com.foodai.backend.repository;

import com.foodai.backend.entity.RecognitionSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecognitionSessionRepository extends JpaRepository<RecognitionSession, Long> {}
