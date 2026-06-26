package com.techmart.service;

import com.techmart.entity.Notification;

import javax.ejb.Local;
import java.util.List;

/**
 * Local EJB interface for Notification management.
 * Used by NotificationMDB and REST layer.
 */
@Local
public interface NotificationService {

    /**
     * Create a new notification record in the database.
     */
    Notification createNotification(Long userId, Notification.NotificationType type,
                                    String subject, String message);

    /**
     * Create and persist an order confirmation notification.
     */
    void sendOrderConfirmation(Long userId, String orderNumber, String details);

    /**
     * Create and persist a shipping update notification.
     */
    void sendShippingUpdate(Long userId, String orderNumber, String trackingInfo);

    /**
     * Create a low-stock alert for warehouse staff.
     */
    void sendLowStockAlert(Long productId, String productName, int currentStock);

    /**
     * Mark a notification as read.
     */
    void markAsRead(Long notificationId);

    /**
     * Mark all notifications as read for a user.
     */
    void markAllAsRead(Long userId);

    /**
     * Find all notifications for a user.
     */
    List<Notification> findByUser(Long userId);

    /**
     * Count unread notifications for a user.
     */
    long countUnread(Long userId);
}
