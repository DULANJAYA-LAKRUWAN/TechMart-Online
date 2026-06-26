package com.techmart.jms;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JMS Producer — sends order processing messages to the OrderQueue.
 *
 * Design:
 * - Uses JMS 2.0 simplified API (@Inject JMSContext) for cleaner code.
 * - JNDI resource: java:/jms/queue/OrderQueue (configured in wildfly-setup.cli)
 * - Message format: TextMessage containing JSON-serialized order data.
 * - setDeliveryMode(PERSISTENT) ensures messages survive broker restarts.
 * - Message priority 9 = high priority for order processing.
 */
@ApplicationScoped
public class OrderQueueProducer {

    private static final Logger LOG = Logger.getLogger(OrderQueueProducer.class.getName());

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "java:/jms/queue/OrderQueue")
    private Queue orderQueue;

    /**
     * Send an order processing message to the queue.
     *
     * @param orderNumber Unique order identifier
     * @param orderJson   JSON payload containing order details for MDB processing
     */
    public void sendOrderForProcessing(String orderNumber, String orderJson) {
        try {
            TextMessage message = jmsContext.createTextMessage(orderJson);
            message.setStringProperty("orderNumber", orderNumber);
            message.setStringProperty("messageType", "ORDER_PROCESS");
            message.setLongProperty("timestamp", System.currentTimeMillis());

            jmsContext.createProducer()
                .setDeliveryMode(DeliveryMode.PERSISTENT)
                .setPriority(9)
                .setProperty("orderNumber", orderNumber)
                .send(orderQueue, message);

            LOG.info("Order queued for processing: " + orderNumber);
        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "Failed to send order to queue: " + orderNumber, e);
            throw new RuntimeException("JMS send failed for order: " + orderNumber, e);
        }
    }

    /**
     * Send a cancellation request to the queue.
     */
    public void sendCancellationRequest(String orderNumber, String reason) {
        try {
            TextMessage message = jmsContext.createTextMessage(
                "{\"action\":\"CANCEL\",\"orderNumber\":\"" + orderNumber
                + "\",\"reason\":\"" + reason + "\"}");
            message.setStringProperty("orderNumber", orderNumber);
            message.setStringProperty("messageType", "ORDER_CANCEL");

            jmsContext.createProducer()
                .setDeliveryMode(DeliveryMode.PERSISTENT)
                .send(orderQueue, message);

            LOG.info("Cancellation request queued for: " + orderNumber);
        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "Failed to queue cancellation for: " + orderNumber, e);
        }
    }
}
