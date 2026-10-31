package com.foodai.backend.dto.response;

import com.foodai.backend.entity.AuditLog;
import com.foodai.backend.entity.AuditLog.Outcome;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * [BE-11] Response DTO cho hiển thị nhật ký kiểm toán (Admin Audit Log UI)
 */
public class AuditLogResponse {

    private Long id;
    private Long actorUserId;
    private String actorEmail;
    private String actionCode;
    private String objectType;
    private Long objectId;
    private Outcome outcome;
    private Map<String, Object> details;
    private LocalDateTime createdAt;

    public AuditLogResponse() {}

    public static AuditLogResponse fromEntity(AuditLog entity) {
        AuditLogResponse res = new AuditLogResponse();
        res.setId(entity.getId());
        if (entity.getActor() != null) {
            res.setActorUserId(entity.getActor().getId());
        }
        res.setActorEmail(entity.getActorEmail());
        res.setActionCode(entity.getActionCode());
        res.setObjectType(entity.getObjectType());
        res.setObjectId(entity.getObjectId());
        res.setOutcome(entity.getOutcome());
        res.setDetails(entity.getDetailsJson());
        res.setCreatedAt(entity.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public String getActionCode() { return actionCode; }
    public void setActionCode(String actionCode) { this.actionCode = actionCode; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public Long getObjectId() { return objectId; }
    public void setObjectId(Long objectId) { this.objectId = objectId; }
    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
