package com.techmart.exception;

/**
 * Thrown when there is not enough stock to fulfill a requested order quantity.
 */
public class InsufficientInventoryException extends TechMartException {

    private static final long serialVersionUID = 1L;

    private final Long productId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientInventoryException(Long productId, int requested, int available) {
        super(String.format(
            "Insufficient stock for product ID %d. Requested: %d, Available: %d",
            productId, requested, available), "INSUFFICIENT_INVENTORY");
        this.productId         = productId;
        this.requestedQuantity = requested;
        this.availableQuantity = available;
    }

    public Long getProductId() { return productId; }
    public int getRequestedQuantity() { return requestedQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
}
