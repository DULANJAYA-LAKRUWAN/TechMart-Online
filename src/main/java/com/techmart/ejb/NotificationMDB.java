package com.techmart.ejb;

import com.techmart.entity.Notification;
import com.techmart.entity.User;
import com.techmart.service.NotificationService;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message-Driven Bean — Notification Dispatcher.
 *
 * Subscribes to: java:/jms/topic/NotificationTopic
 * Subscription: Non-durable (messages missed while down are not redelivered).
 *
 * Design:
 * - Topic subscription means ALL active MDB instances receive every message (broadcast).
 * - Parses pipe-delimited notification payload from the topic.
 * - Persists notification record to DB.
 * - Stubs email delivery (JavaMail stub logs to console; real impl would use SMTP).
 * - Multiple concurrent instances (maxSession=5) handle high notification volume.
 *
 * Message Format: "TYPE=ORDER_CONFIRMATION|ORDER=TM-xxx|USER_ID=1|EMAIL=user@example.com"
 */
@MessageDriven(
    name = "NotificationMDB",
    activationConfig = {
        @ActivationConfigProperty(
            propertyName  = "destinationType",
            propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(
            propertyName  = "destination",
            propertyValue = "java:/jms/topic/NotificationTopic"),
        @ActivationConfigProperty(
            propertyName  = "acknowledgeMode",
            propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(
            propertyName  = "subscriptionDurability",
            propertyValue = "NonDurable"),
        @ActivationConfigProperty(
            propertyName  = "maxSession",
            propertyValue = "5")
    }
)
public class NotificationMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(NotificationMDB.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                LOG.warning("NotificationMDB: Non-TextMessage received — skipping.");
                return;
            }

            String body    = ((TextMessage) message).getText();
            String subject = message.getStringProperty("subject");
            LOG.info("NotificationMDB: Received notification: " + subject);

            // Parse payload: "TYPE=X|ORDER=Y|USER_ID=Z|EMAIL=W"
            NotificationPayload payload = parsePayload(body);

            // Dispatch based on type
            switch (payload.type) {
                case "ORDER_CONFIRMATION":
                    handleOrderConfirmation(payload, subject);
                    break;
                case "SHIPPING_UPDATE":
                    handleShippingUpdate(payload, subject);
                    break;
                case "LOW_STOCK_ALERT":
                    handleLowStockAlert(payload, subject);
                    break;
                default:
                    handleGenericNotification(payload, subject, body);
            }

        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "NotificationMDB: JMS read error", e);
            throw new RuntimeException("JMS message read failed", e);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "NotificationMDB: Processing error", e);
            // Do NOT throw — notification failures should not cause message redelivery
        }
    }

    // ── Notification Handlers ─────────────────────────────────────────────────

    private void handleOrderConfirmation(NotificationPayload payload, String subject) {
        Long userId = payload.getUserId();
        if (userId == null) return;

        User user = em.find(User.class, userId);
        if (user == null) return;

        String message = "Your order " + payload.get("ORDER")
            + " has been confirmed. Thank you for shopping with TechMart Online!";

        Notification notification = new Notification(
            user, Notification.NotificationType.ORDER_CONFIRMATION, subject, message);
        notification.setReferenceType("Order");
        em.persist(notification);

        // Stub: JavaMail / SMTP email delivery
        sendEmailStub(payload.get("EMAIL"), subject, message);

        LOG.info("NotificationMDB: Order confirmation notification saved for user: " + userId);
    }

    private void handleShippingUpdate(NotificationPayload payload, String subject) {
        Long userId = payload.getUserId();
        if (userId == null) return;

        User user = em.find(User.class, userId);
        if (user == null) return;

        String message = "Your order " + payload.get("ORDER")
            + " has been shipped. Tracking: " + payload.get("TRACKING");

        Notification notification = new Notification(
            user, Notification.NotificationType.SHIPPING_UPDATE, subject, message);
        em.persist(notification);

        sendEmailStub(user.getEmail(), subject, message);
        LOG.info("NotificationMDB: Shipping update saved for user: " + userId);
    }

    private void handleLowStockAlert(NotificationPayload payload, String subject) {
        // Admin-only alert — persist without a specific user (system notification)
        String message = "Product [" + payload.get("PRODUCT_NAME")
            + "] (ID: " + payload.get("PRODUCT_ID") + ") is low on stock. "
            + "Current quantity: " + payload.get("STOCK");

        Notification notification = new Notification(
            null, Notification.NotificationType.LOW_STOCK_ALERT, subject, message);
        em.persist(notification);

        LOG.warning("NotificationMDB: LOW STOCK ALERT — " + message);
    }

    private void handleGenericNotification(NotificationPayload payload, String subject, String body) {
        Notification notification = new Notification(
            null, Notification.NotificationType.SYSTEM_ALERT, subject, body);
        em.persist(notification);
        LOG.info("NotificationMDB: Generic notification saved.");
    }

    // ── Email Stub ────────────────────────────────────────────────────────────

    private void sendEmailStub(String to, String subject, String body) {
        // JavaMail stub — in production: inject @Resource MailSession and send via SMTP
        LOG.info("=== EMAIL STUB ===");
        LOG.info("  TO:      " + to);
        LOG.info("  SUBJECT: " + subject);
        LOG.info("  BODY:    " + body);
        LOG.info("=================");
    }

    // ── Payload Parser ────────────────────────────────────────────────────────

    private NotificationPayload parsePayload(String body) {
        NotificationPayload payload = new NotificationPayload();
        if (body == null || body.isEmpty()) return payload;

        String[] parts = body.split("\\|");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                payload.data.put(kv[0].trim(), kv[1].trim());
            }
        }
        payload.type = payload.get("TYPE");
        return payload;
    }

    /** Simple key-value store for parsed notification payload */
    private static class NotificationPayload {
        String type = "UNKNOWN";
        final java.util.Map<String, String> data = new java.util.HashMap<>();

        String get(String key) { return data.getOrDefault(key, ""); }

        Long getUserId() {
            String v = get("USER_ID");
            if (v.isEmpty()) return null;
            try { return Long.parseLong(v); } catch (NumberFormatException e) { return null; }
        }
    }
}
