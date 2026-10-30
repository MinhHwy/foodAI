package com.foodai.backend.service;

import com.foodai.backend.dto.request.CreateAuditLogRequest;
import com.foodai.backend.dto.response.AuditLogResponse;
import com.foodai.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * [BE-11] Service quản lý ghi và truy vấn nhật ký kiểm toán bảo mật (FR14, NFR08)
 */
public interface AuditLogService {

    /**
     * Ghi nhật ký thao tác nhạy cảm với cơ chế khử trùng dữ liệu bí mật (Sanitization)
     */
    AuditLogResponse recordAuditLog(Long actorUserId, String actorEmail, String actionCode,
                                    String objectType, Long objectId, AuditLog.Outcome outcome,
                                    Map<String, Object> details);

    /**
     * Ghi nhật ký từ request DTO
     */
    AuditLogResponse recordAuditLog(CreateAuditLogRequest request);

    /**
     * Lấy danh sách nhật ký kiểm toán có phân trang
     */
    Page<AuditLogResponse> getAllAuditLogs(Pageable pageable);

    /**
     * Lọc nhật ký kiểm toán theo mã hành động (actionCode)
     */
    Page<AuditLogResponse> getAuditLogsByAction(String actionCode, Pageable pageable);

    /**
     * Lọc nhật ký kiểm toán theo người thực hiện (actorUserId)
     */
    Page<AuditLogResponse> getAuditLogsByUser(Long userId, Pageable pageable);
}
