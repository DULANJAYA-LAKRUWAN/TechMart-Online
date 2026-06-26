package com.techmart.ejb;

import com.techmart.entity.AuditLog;
import com.techmart.entity.Inventory;
import com.techmart.entity.Order;
import com.techmart.jms.NotificationTopicPublisher;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message-Driven Bean — Order Processing.
 *
 * Listens on: java:/jms/queue/OrderQueue
 *
 * Design Decisions:
 * - MDB decouples order placement from fulfillment — REST returns immediately after
 *   placing the order in the queue; this bean processes it asynchronously.
 * - Container-managed transaction: if processing fails, message is returned to queue
 *   (redelivered up to maxSession times before going to Dead Letter Queue).
 * - Processes: inventory deduction, order status update, audit logging.
 * - Delegates notification to NotificationTopicPublisher (topic fan-out).
 *
 * Performance:
 * - maxSession=10: up to 10 concurrent MDB instances process queue in parallel.
 * - This is the key scalability mechanism for order throughput.
 */
@MessageDriven(
    name = "OrderProcessingMDB",
    activationConfig = {
        @ActivationConfigProperty(
            propertyName  = "destinationType",
            propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(
            propertyName  = "destination",
            propertyValue = "java:/jms/queue/OrderQueue"),
        @ActivationConfigProperty(
            propertyName  = "acknowledgeMode",
            propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(
            propertyName  = "maxSession",
            propertyValue = "10")
    }
)
public class OrderProcessingMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(OrderProcessingMDB.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @Inject
    private NotificationTopicPublisher topicPublisher;

    @Inject
    private InventoryCacheBean inventoryCache;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        String orderNumber = null;
        try {
            if (!(message instanceof TextMessage)) {
                LOG.warning("OrderProcessingMDB: Received non-TextMessage — skipping.");
                return;
            }

            TextMessage textMessage = (TextMessage) message;
            orderNumber = message.getStringProperty("orderNumber");
            String messageType = message.getStringProperty("messageType");
            String body = textMessage.getText();

            LOG.info("OrderProcessingMDB: Processing [" + messageType + "] for order: " + orderNumber);

            if ("ORDER_CANCEL".equals(messageType)) {
                processCancellation(orderNumber, body);
            } else {
                processOrder(orderNumber, body);
            }

        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "OrderProcessingMDB: JMS read error", e);
            throw new RuntimeException("JMS message read failed", e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "OrderProcessingMDB: Processing failed for order: " + orderNumber, e);
            // RuntimeException triggers JTA rollback → message redelivery
            throw new RuntimeException("Order processing failed: " + orderNumber, e);
        }
    }

    private void processOrder(String orderNumber, String orderJson) {
        // 1. Load order from DB
        Order order = findOrderByNumber(orderNumber);
        if (order == null) {
            LOG.warning("OrderProcessingMDB: Order not found: " + orderNumber);
            return;
        }

        // 2. Deduct inventory for each line item
        boolean inventoryOk = deductInventory(order);
        if (!inventoryOk) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setNotes("Auto-cancelled: insufficient inventory at processing time.");
            em.merge(order);
            persistAuditLog("ORDER_AUTO_CANCELLED", "Order", order.getId(),
                "Insufficient inventory — order auto-cancelled: " + orderNumber, false);
            return;
        }

        // 3. Simulate payment validation (stub)
        boolean paymentOk = validatePayment(order);
        if (!paymentOk) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            em.merge(order);
            persistAuditLog("ORDER_PAYMENT_FAILED", "Order", order.getId(),
                "Payment validation failed for: " + orderNumber, false);
            return;
        }

        // 4. Confirm order
        order.setStatus(Order.OrderStatus.CONFIRMED);
        em.merge(order);

        // 5. Persist audit log
        persistAuditLog("ORDER_CONFIRMED", "Order", order.getId(),
            "Order processed and confirmed: " + orderNumber, true);

        // 6. Notify via topic
        topicPublisher.publishOrderConfirmation(
            orderNumber,
            order.getUser().getId(),
            order.getUser().getEmail());

        LOG.info("OrderProcessingMDB: Successfully processed order: " + orderNumber);
    }

    private void processCancellation(String orderNumber, String body) {
        Order order = findOrderByNumber(orderNumber);
        if (order == null) return;

        // Release reserved inventory
        releaseInventory(order);

        order.setStatus(Order.OrderStatus.CANCELLED);
        em.merge(order);

        persistAuditLog("ORDER_CANCELLED", "Order", order.getId(),
            "Order cancelled: " + orderNumber, true);

        LOG.info("OrderProcessingMDB: Order cancelled and inventory released: " + orderNumber);
    }

    private boolean deductInventory(Order order) {
        try {
            for (com.techmart.entity.OrderItem item : order.getItems()) {
                Long productId = item.getProduct().getId();
                List<Inventory> inventoryList = em.createNamedQuery(
                    "Inventory.findByProduct", Inventory.class)
                    .setParameter("productId", productId)
                    .getResultList();

                int remaining = item.getQuantity();
                for (Inventory inv : inventoryList) {
                    if (remaining <= 0) break;
                    int deductQty = Math.min(remaining, inv.getAvailableQuantity());
                    if (deductQty > 0) {
                        inv.deduct(deductQty);
                        em.merge(inv);
                        remaining -= deductQty;
                    }
                }

                if (remaining > 0) {
                    LOG.warning("Insufficient inventory for product: " + productId);
                    // Refresh cache after stock change
                    inventoryCache.refreshProduct(productId);
                    return false;
                }

                // Refresh cache after deduction
                inventoryCache.refreshProduct(productId);

                // Check for low stock alert
                int newStock = inventoryCache.getAvailableStock(productId);
                if (newStock <= 10) {
                    topicPublisher.publishLowStockAlert(
                        productId, item.getProductNameSnapshot(), newStock);
                }
            }
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Inventory deduction failed", e);
            return false;
        }
    }

    private void releaseInventory(Order order) {
        // Stub: In production, reverse inventory reservations
        LOG.info("Inventory release for cancelled order: " + order.getOrderNumber());
    }

    private boolean validatePayment(Order order) {
        // Stub: In production, call payment gateway (Stripe, PayPal, etc.)
        // Returns true for demo purposes
        LOG.info("Payment validation stub: APPROVED for order " + order.getOrderNumber());
        return true;
    }

    private Order findOrderByNumber(String orderNumber) {
        try {
            return em.createNamedQuery("Order.findByOrderNumber", Order.class)
                .setParameter("orderNumber", orderNumber)
                .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private void persistAuditLog(String action, String entity, Long entityId,
                                  String description, boolean success) {
        try {
            AuditLog log = new AuditLog(action, entity, entityId, success);
            log.withDescription(description);
            em.persist(log);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to persist audit log in MDB", e);
        }
    }
}
