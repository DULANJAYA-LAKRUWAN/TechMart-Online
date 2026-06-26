package com.techmart.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Order details display and API responses.
 */
public class OrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private String shippingAddress;
    private String paymentMethod;
    private Long userId;
    private String userFullName;
    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private List<OrderItemDTO> items;

    // ── Inner DTO ────────────────────────────────────────────────────────────

    public static class OrderItemDTO implements Serializable {
        private Long productId;
        private String productName;
        private String productSku;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;

        public OrderItemDTO() {}

        public OrderItemDTO(Long productId, String productName, String sku,
                            int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
            this.productId   = productId;
            this.productName = productName;
            this.productSku  = sku;
            this.quantity    = quantity;
            this.unitPrice   = unitPrice;
            this.subtotal    = subtotal;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductSku() { return productSku; }
        public void setProductSku(String productSku) { this.productSku = productSku; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public OrderDTO() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }
}
