package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA Entity representing system or user notifications.
 * Maps to the 'notifications' table.
 *
 * Types: ORDER_CONFIRMATION, SHIPPING_UPDATE, LOW_STOCK_ALERT, SYSTEM_ALERT, PROMOTION
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_user_id",    columnList = "user_id"),
        @Index(name = "idx_notifications_type",       columnList = "type"),
        @Index(name = "idx_notifications_read",       columnList = "is_read"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at")
    }
)
@NamedQueries({
    @NamedQuery(name = "Notification.findByUser",
        query = "SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC"),
    @NamedQuery(name = "Notification.findUnreadByUser",
        query = "SELECT n FROM Notification n WHERE n.user.id = :userId AND n.read = false ORDER BY n.createdAt DESC"),
    @NamedQuery(name = "Notification.countUnreadByUser",
        query = "SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.read = false"),
    @NamedQuery(name = "Notification.findByType",
        query = "SELECT n FROM Notification n WHERE n.type = :type ORDER BY n.createdAt DESC")
})
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum NotificationType {
        ORDER_CONFIRMATION, SHIPPING_UPDATE, DELIVERY_CONFIRMATION,
        LOW_STOCK_ALERT, SYSTEM_ALERT, PROMOTION, ACCOUNT_UPDATE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq")
    @SequenceGenerator(name = "notification_seq", sequenceName = "notifications_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @NotBlank
    @Size(max = 200)
    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @NotBlank
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** User who receives this notification (null = system broadcast) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public Notification() {}

    public Notification(User user, NotificationType type, String subject, String message) {
        this.user    = user;
        this.type    = type;
        this.subject = subject;
        this.message = message;
    }

    // ── Business Methods ─────────────────────────────────────────────────────

    public void markAsRead() {
        this.read = true;
    }

    public void markEmailSent() {
        this.emailSent    = true;
        this.emailSentAt  = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public boolean isEmailSent() { return emailSent; }

    public LocalDateTime getEmailSentAt() { return emailSentAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        Notification that = (Notification) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Notification{id=" + id + ", type=" + type + ", subject='" + subject + "'}";
    }
}
