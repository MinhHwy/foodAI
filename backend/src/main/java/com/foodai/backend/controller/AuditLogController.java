package com.foodai.backend.controller;

import com.foodai.backend.dto.request.CreateAuditLogRequest;
import com.foodai.backend.dto.response.ApiResponse;
import com.foodai.backend.dto.response.AuditLogResponse;
import com.foodai.backend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * [BE-11] REST Controller quản lý API nhật ký kiểm toán bảo mật cho Admin & Hệ thống
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Lấy danh sách nhật ký kiểm toán có phân trang và lọc theo actionCode hoặc userId
     * Endpoint: GET /api/v1/audit-logs?page=0&size=20&actionCode=USER_LOGIN&userId=1
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) Long userId) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AuditLogResponse> logs;

        if (actionCode != null && !actionCode.trim().isEmpty()) {
            logs = auditLogService.getAuditLogsByAction(actionCode.trim(), pageable);
        } else if (userId != null) {
            logs = auditLogService.getAuditLogsByUser(userId, pageable);
        } else {
            logs = auditLogService.getAllAuditLogs(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhật ký kiểm toán thành công.", logs));
    }

    /**
     * Ghi nhật ký thao tác nhạy cảm
     * Endpoint: POST /api/v1/audit-logs
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AuditLogResponse>> createAuditLog(
            @Valid @RequestBody CreateAuditLogRequest request) {
        AuditLogResponse recorded = auditLogService.recordAuditLog(request);
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận nhật ký kiểm toán thành công.", recorded));
    }

    /**
     * Danh sách các mã hành động nhạy cảm tiêu biểu trong hệ thống (phục vụ bộ lọc UI Admin)
     * Endpoint: GET /api/v1/audit-logs/actions
     */
    @GetMapping("/actions")
    public ResponseEntity<ApiResponse<List<String>>> getSupportedActions() {
        List<String> actions = Arrays.asList(
                "USER_REGISTER",
                "USER_LOGIN",
                "USER_LOGOUT",
                "PASSWORD_CHANGE",
                "PASSWORD_RESET_REQUEST",
                "PROFILE_UPDATE",
                "GOAL_UPDATE",
                "FOOD_IMAGE_UPLOAD",
                "FOOD_DIARY_CREATE",
                "FOOD_DIARY_DELETE",
                "ADMIN_FOOD_CREATE",
                "ADMIN_FOOD_UPDATE",
                "ADMIN_FOOD_DELETE",
                "ADMIN_USER_SUSPEND"
        );
        return ResponseEntity.ok(ApiResponse.success("Danh sách mã hành động kiểm toán.", actions));
    }
}
