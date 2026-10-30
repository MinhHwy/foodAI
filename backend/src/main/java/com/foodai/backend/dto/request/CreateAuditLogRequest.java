package com.foodai.backend.dto.request;

import com.foodai.backend.entity.AuditLog.Outcome;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * [BE-11] Request DTO để ghi nhật ký thao tác nhạy cảm
 */
public class CreateAuditLogRequest {

    private Long actorUserId;
    private String actorEmail;

    @NotBlank(message = "actionCode không được để trống")
    private String actionCode;

    private String objectType;
    private Long objectId;
    private Outcome outcome = Outcome.SUCCESS;
    private Map<String, Object> details;

    public CreateAuditLogRequest() {}

    public CreateAuditLogRequest(Long actorUserId, String actorEmail, String actionCode,
                                 String objectType, Long objectId, Outcome outcome, Map<String, Object> details) {
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.actionCode = actionCode;
        this.objectType = objectType;
        this.objectId = objectId;
        this.outcome = outcome;
        this.details = details;
    }

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
}
