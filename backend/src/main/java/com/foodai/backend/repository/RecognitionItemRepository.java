package com.foodai.backend.repository;

import com.foodai.backend.entity.RecognitionItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecognitionItemRepository extends JpaRepository<RecognitionItem, Long> {
    List<RecognitionItem> findBySessionIdOrderByIdAsc(Long sessionId);
    boolean existsBySessionId(Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from RecognitionItem i where i.id = :id")
    Optional<RecognitionItem> findLockedById(@Param("id") Long id);
}
