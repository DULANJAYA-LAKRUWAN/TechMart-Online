package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA Entity representing an audit log entry.
 * Maps to the 'audit_logs' table.
 *
 * Populated by AuditInterceptor (CDI @Interceptor) on every @Auditable EJB method.
 * Provides a tamper-resistant trail for compliance and debugging.
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_user_id",    columnList = "user_id"),
        @Index(name = "idx_audit_action",     columnList = "action"),
        @Index(name = "idx_audit_entity",     columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_success",    columnList = "success")
    }
)
@NamedQueries({
    @NamedQuery(name = "AuditLog.findAll",
        query = "SELECT a FROM AuditLog a ORDER BY a.createdAt DESC"),
    @NamedQuery(name = "AuditLog.findByUser",
        query = "SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC"),
    @NamedQuery(name = "AuditLog.findByAction",
        query = "SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.createdAt DESC"),
    @NamedQuery(name = "AuditLog.findByEntityType",
        query = "SELECT a FROM AuditLog a WHERE a.entityType = :entityType ORDER BY a.createdAt DESC"),
    @NamedQuery(name = "AuditLog.findFailures",
        query = "SELECT a FROM AuditLog a WHERE a.success = false ORDER BY a.createdAt DESC"),
    @NamedQuery(name = "AuditLog.findSlowOperations",
        query = "SELECT a FROM AuditLog a WHERE a.executionTimeMs > :threshold ORDER BY a.executionTimeMs DESC")
})
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_seq")
    @SequenceGenerator(name = "audit_seq", sequenceName = "audit_logs_id_seq", allocationSize = 10)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "success", nullable = false)
    private boolean success = true;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public AuditLog() {}

    public AuditLog(String action, String entityType, Long entityId, boolean success) {
        this.action     = action;
        this.entityType = entityType;
        this.entityId   = entityId;
        this.success    = success;
    }

    // ── Builder-style convenience setters ────────────────────────────────────

    public AuditLog withUser(Long userId, String username) {
        this.userId   = userId;
        this.username = username;
        return this;
    }

    public AuditLog withDescription(String description) {
        this.description = description;
        return this;
    }

    public AuditLog withExecutionTime(long ms) {
        this.executionTimeMs = ms;
        return this;
    }

    public AuditLog withValues(String oldValue, String newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
        return this;
    }

    public AuditLog withError(String errorMessage) {
        this.success      = false;
        this.errorMessage = errorMessage;
        return this;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "AuditLog{id=" + id + ", action='" + action + "', entity=" + entityType
            + "#" + entityId + ", success=" + success + "}";
    }
}
