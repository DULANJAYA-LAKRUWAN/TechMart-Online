package com.techmart.dto;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.List;

/**
 * Request DTO for placing an order via REST API.
 * Validated before being passed to OrderServiceBean.
 */
public class OrderRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotEmpty(message = "At least one item is required")
    private List<OrderItemRequest> items;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    private String billingAddress;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String notes;

    // ── Inner class ──────────────────────────────────────────────────────────

    public static class OrderItemRequest implements Serializable {

        @NotNull(message = "Product ID is required")
        private Long productId;

        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100, message = "Quantity cannot exceed 100 per item")
        private int quantity;

        public OrderItemRequest() {}

        public OrderItemRequest(Long productId, int quantity) {
            this.productId = productId;
            this.quantity  = quantity;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public OrderRequestDTO() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
