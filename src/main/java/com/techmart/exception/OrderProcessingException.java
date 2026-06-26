package com.techmart.exception;

/**
 * Thrown when order processing fails (e.g. invalid state transition, payment failure).
 */
public class OrderProcessingException extends TechMartException {

    private static final long serialVersionUID = 1L;

    private final String orderNumber;

    public OrderProcessingException(String message) {
        super(message, "ORDER_PROCESSING_ERROR");
        this.orderNumber = null;
    }

    public OrderProcessingException(String orderNumber, String message) {
        super("Order [" + orderNumber + "] processing failed: " + message, "ORDER_PROCESSING_ERROR");
        this.orderNumber = orderNumber;
    }

    public OrderProcessingException(String orderNumber, String message, Throwable cause) {
        super("Order [" + orderNumber + "] processing failed: " + message, "ORDER_PROCESSING_ERROR", cause);
        this.orderNumber = orderNumber;
    }

    public String getOrderNumber() { return orderNumber; }
}
