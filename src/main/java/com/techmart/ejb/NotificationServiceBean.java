package com.techmart.ejb;

import com.techmart.entity.Notification;
import com.techmart.entity.User;
import com.techmart.service.NotificationService;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.logging.Logger;

/**
 * Stateless Session Bean — Notification Management.
 * Implements NotificationService for direct notification creation from REST/EJB layer.
 */
@Stateless
public class NotificationServiceBean implements NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationServiceBean.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @Override
    public Notification createNotification(Long userId, Notification.NotificationType type,
                                           String subject, String message) {
        User user = userId != null ? em.find(User.class, userId) : null;
        Notification notification = new Notification(user, type, subject, message);
        em.persist(notification);
        return notification;
    }

    @Override
    public void sendOrderConfirmation(Long userId, String orderNumber, String details) {
        String subject = "Order Confirmed: " + orderNumber;
        String body    = "Your TechMart order " + orderNumber + " is confirmed. " + details;
        createNotification(userId, Notification.NotificationType.ORDER_CONFIRMATION, subject, body);
        LOG.info("Order confirmation notification created for user: " + userId);
    }

    @Override
    public void sendShippingUpdate(Long userId, String orderNumber, String trackingInfo) {
        String subject = "Shipped: Order " + orderNumber;
        String body    = "Your order " + orderNumber + " has been shipped. Tracking: " + trackingInfo;
        createNotification(userId, Notification.NotificationType.SHIPPING_UPDATE, subject, body);
    }

    @Override
    public void sendLowStockAlert(Long productId, String productName, int currentStock) {
        String subject = "Low Stock Alert: " + productName;
        String body    = "Product '" + productName + "' (ID:" + productId
            + ") has only " + currentStock + " units remaining.";
        createNotification(null, Notification.NotificationType.LOW_STOCK_ALERT, subject, body);
    }

    @Override
    public void markAsRead(Long notificationId) {
        Notification n = em.find(Notification.class, notificationId);
        if (n != null) {
            n.markAsRead();
            em.merge(n);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        em.createQuery("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
    }

    @Override
    public List<Notification> findByUser(Long userId) {
        return em.createNamedQuery("Notification.findByUser", Notification.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long countUnread(Long userId) {
        return em.createNamedQuery("Notification.countUnreadByUser", Long.class)
            .setParameter("userId", userId)
            .getSingleResult();
    }
}
