package com.foodai.backend.repository;

import com.foodai.backend.entity.RecognitionSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RecognitionSessionRepository extends JpaRepository<RecognitionSession, Long> {
    Optional<RecognitionSession> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RecognitionSession s where s.id = :id")
    Optional<RecognitionSession> findLockedById(@Param("id") Long id);
}
