package com.techmart.cdi;

import com.techmart.jms.NotificationTopicPublisher;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * CDI Event Observer — reacts to OrderPlacedEvent fired by OrderServiceBean.
 *
 * This bean is @ApplicationScoped for efficiency.
 * The @Observes method is invoked synchronously in the same transaction as the event.
 *
 * Responsibilities:
 * - Publish to NotificationTopic (fan-out to all NotificationMDB subscribers)
 * - Log the event for debugging
 */
@ApplicationScoped
public class OrderEventObserver {

    private static final Logger LOG = Logger.getLogger(OrderEventObserver.class.getName());

    @Inject
    private NotificationTopicPublisher topicPublisher;

    /**
     * Called automatically by CDI whenever an OrderPlacedEvent is fired.
     * The @Observes annotation wires this as an event consumer.
     */
    public void onOrderPlaced(@Observes OrderPlacedEvent event) {
        LOG.info("CDI Event received: " + event);

        String message = String.format(
            "ORDER_CONFIRMATION:userId=%d:orderNumber=%s:total=%.2f:email=%s",
            event.getUserId(),
            event.getOrderNumber(),
            event.getTotalAmount(),
            event.getUserEmail()
        );

        topicPublisher.publish(event.getOrderNumber(), message);

        LOG.info("Published notification to topic for order: " + event.getOrderNumber());
    }
}
