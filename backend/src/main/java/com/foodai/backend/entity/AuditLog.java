package com.foodai.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * [BE-11] Entity AuditLog
 * Ghi nhận nhật ký an ninh/kiểm toán cho các thao tác nhạy cảm (FR14, NFR08)
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    public enum Outcome {
        SUCCESS,
        FAILURE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(name = "actor_email", length = 190)
    private String actorEmail;

    @Column(name = "action_code", nullable = false, length = 100)
    private String actionCode;

    @Column(name = "object_type", length = 80)
    private String objectType;

    @Column(name = "object_id")
    private Long objectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private Outcome outcome = Outcome.SUCCESS;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details_json", columnDefinition = "JSON")
    private Map<String, Object> detailsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLog() {}

    public AuditLog(User actor, String actorEmail, String actionCode, String objectType,
                    Long objectId, Outcome outcome, Map<String, Object> detailsJson) {
        this.actor = actor;
        this.actorEmail = actorEmail;
        this.actionCode = actionCode;
        this.objectType = objectType;
        this.objectId = objectId;
        this.outcome = outcome;
        this.detailsJson = detailsJson;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
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
    public Map<String, Object> getDetailsJson() { return detailsJson; }
    public void setDetailsJson(Map<String, Object> detailsJson) { this.detailsJson = detailsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
