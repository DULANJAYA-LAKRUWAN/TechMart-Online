package com.techmart.jms;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JMS Topic Publisher — publishes notification events to the NotificationTopic.
 *
 * Design:
 * - Publish-Subscribe (Topic) model: all active subscribers (NotificationMDB, future
 *   SMS/push providers) receive every message — true fan-out.
 * - NON_PERSISTENT delivery acceptable for notifications (best effort).
 * - JNDI resource: java:/jms/topic/NotificationTopic
 */
@ApplicationScoped
public class NotificationTopicPublisher {

    private static final Logger LOG = Logger.getLogger(NotificationTopicPublisher.class.getName());

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "java:/jms/topic/NotificationTopic")
    private Topic notificationTopic;

    /**
     * Publish a notification event to all topic subscribers.
     *
     * @param subject Short subject/title of the notification
     * @param message Full message body (pipe-delimited format for MDB parsing)
     */
    public void publish(String subject, String message) {
        try {
            TextMessage textMessage = jmsContext.createTextMessage(message);
            textMessage.setStringProperty("subject", subject);
            textMessage.setStringProperty("messageType", "NOTIFICATION");
            textMessage.setLongProperty("publishedAt", System.currentTimeMillis());

            jmsContext.createProducer()
                .setDeliveryMode(DeliveryMode.NON_PERSISTENT)
                .send(notificationTopic, textMessage);

            LOG.info("Notification published to topic: " + subject);
        } catch (JMSException e) {
            LOG.log(Level.WARNING, "Failed to publish notification to topic", e);
            // Notification failure must not propagate to the calling transaction
        }
    }

    /**
     * Publish an order confirmation notification.
     */
    public void publishOrderConfirmation(String orderNumber, Long userId, String email) {
        String payload = String.format(
            "TYPE=ORDER_CONFIRMATION|ORDER=%s|USER_ID=%d|EMAIL=%s",
            orderNumber, userId, email);
        publish("Order Confirmed: " + orderNumber, payload);
    }

    /**
     * Publish a low-stock alert for admin users.
     */
    public void publishLowStockAlert(Long productId, String productName, int stock) {
        String payload = String.format(
            "TYPE=LOW_STOCK_ALERT|PRODUCT_ID=%d|PRODUCT_NAME=%s|STOCK=%d",
            productId, productName, stock);
        publish("Low Stock Alert: " + productName, payload);
    }

    /**
     * Publish a shipping update.
     */
    public void publishShippingUpdate(String orderNumber, Long userId, String trackingNo) {
        String payload = String.format(
            "TYPE=SHIPPING_UPDATE|ORDER=%s|USER_ID=%d|TRACKING=%s",
            orderNumber, userId, trackingNo);
        publish("Shipment Update: " + orderNumber, payload);
    }
}
