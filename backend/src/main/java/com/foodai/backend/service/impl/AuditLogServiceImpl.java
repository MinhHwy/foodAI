package com.foodai.backend.service.impl;

import com.foodai.backend.dto.request.CreateAuditLogRequest;
import com.foodai.backend.dto.response.AuditLogResponse;
import com.foodai.backend.entity.AuditLog;
import com.foodai.backend.entity.User;
import com.foodai.backend.repository.AuditLogRepository;
import com.foodai.backend.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * [BE-11] Triển khai ghi nhật ký kiểm toán và khử trùng dữ liệu nhạy cảm (NFR08)
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    // Danh sách các từ khóa dữ liệu bí mật cần khử trùng (Masking/Removal) theo tiêu chuẩn OWASP / Proposal NFR08
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "password_hash", "pass", "pwd", "token", "access_token",
            "refresh_token", "secret", "api_secret", "code_hash", "raw_code", "authorization"
    ));

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public AuditLogResponse recordAuditLog(Long actorUserId, String actorEmail, String actionCode,
                                           String objectType, Long objectId, AuditLog.Outcome outcome,
                                           Map<String, Object> details) {

        // Khử trùng các trường nhạy cảm để đảm bảo NFR08 không ghi lọt mật khẩu hay token vào log
        Map<String, Object> sanitizedDetails = sanitizeDetails(details);

        User actor = null;
        if (actorUserId != null) {
            actor = new User(actorUserId);
        }

        AuditLog auditLog = new AuditLog(
                actor,
                actorEmail,
                actionCode,
                objectType,
                objectId,
                outcome != null ? outcome : AuditLog.Outcome.SUCCESS,
                sanitizedDetails
        );

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("[AUDIT] Action: {} | Actor: {} | Outcome: {}", actionCode, actorEmail, auditLog.getOutcome());

        return AuditLogResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public AuditLogResponse recordAuditLog(CreateAuditLogRequest request) {
        return recordAuditLog(
                request.getActorUserId(),
                request.getActorEmail(),
                request.getActionCode(),
                request.getObjectType(),
                request.getObjectId(),
                request.getOutcome(),
                request.getDetails()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByAction(String actionCode, Pageable pageable) {
        return auditLogRepository.findByActionCodeOrderByCreatedAtDesc(actionCode, pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(userId, pageable)
                .map(AuditLogResponse::fromEntity);
    }

    /**
     * Khử trùng dữ liệu chi tiết nhằm chống rò rỉ dữ liệu nhạy cảm (NFR08 & OWASP Top 10)
     */
    private Map<String, Object> sanitizeDetails(Map<String, Object> rawDetails) {
        if (rawDetails == null || rawDetails.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> cleanMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawDetails.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;

            if (SENSITIVE_KEYS.contains(key.toLowerCase().trim())) {
                cleanMap.put(key, "******[REDACTED]******");
            } else {
                cleanMap.put(key, entry.getValue());
            }
        }
        return cleanMap;
    }
}
