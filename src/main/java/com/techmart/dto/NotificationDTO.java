package com.techmart.dto;

import com.techmart.entity.Notification;
import com.techmart.entity.Notification.NotificationType;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for {@link Notification} entity.
 * Provides a lightweight representation suitable for REST responses.
 */
public class NotificationDTO {
    private Long id;
    private NotificationType type;
    private String subject;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private Long referenceId;
    private String referenceType;
    private Long userId;

    public NotificationDTO() {}

    public NotificationDTO(Long id, NotificationType type, String subject, String message,
                           boolean read, LocalDateTime createdAt, Long referenceId,
                           String referenceType, Long userId) {
        this.id = id;
        this.type = type;
        this.subject = subject;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.userId = userId;
    }

    public static NotificationDTO fromEntity(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationDTO(
                notification.getId(),
                notification.getType(),
                notification.getSubject(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReferenceId(),
                notification.getReferenceType(),
                notification.getUser() != null ? notification.getUser().getId() : null
        );
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
