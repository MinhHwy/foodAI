package com.foodai.backend.repository;

import com.foodai.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByActionCodeOrderByCreatedAtDesc(String actionCode, Pageable pageable);

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorUserId, Pageable pageable);

    Page<AuditLog> findByActorEmailOrderByCreatedAtDesc(String actorEmail, Pageable pageable);
}
