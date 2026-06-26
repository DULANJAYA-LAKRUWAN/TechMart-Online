package com.techmart.cdi;

import java.io.Serializable;

/**
 * CDI Event payload fired when an order is successfully placed.
 * Observed by OrderEventObserver to trigger downstream notification processing.
 *
 * This decouples OrderServiceBean from NotificationService —
 * the order service fires an event and does not need to know about notification delivery.
 */
public class OrderPlacedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long    orderId;
    private final String  orderNumber;
    private final Long    userId;
    private final String  userEmail;
    private final double  totalAmount;

    public OrderPlacedEvent(Long orderId, String orderNumber, Long userId,
                            String userEmail, double totalAmount) {
        this.orderId     = orderId;
        this.orderNumber = orderNumber;
        this.userId      = userId;
        this.userEmail   = userEmail;
        this.totalAmount = totalAmount;
    }

    public Long    getOrderId()     { return orderId;     }
    public String  getOrderNumber() { return orderNumber; }
    public Long    getUserId()      { return userId;      }
    public String  getUserEmail()   { return userEmail;   }
    public double  getTotalAmount() { return totalAmount; }

    @Override
    public String toString() {
        return "OrderPlacedEvent{orderId=" + orderId + ", orderNumber='" + orderNumber
            + "', userId=" + userId + ", total=" + totalAmount + "}";
    }
}
