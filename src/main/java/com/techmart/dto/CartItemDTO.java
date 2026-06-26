package com.techmart.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO representing a single item in the shopping cart.
 * Used by ShoppingCartBean (Stateful EJB) for serialization across session.
 */
public class CartItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private String productSku;
    private String imageUrl;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // ── Constructors ─────────────────────────────────────────────────────────

    public CartItemDTO() {}

    public CartItemDTO(Long productId, String productName, String productSku,
                       int quantity, BigDecimal unitPrice) {
        this.productId   = productId;
        this.productName = productName;
        this.productSku  = productSku;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
        this.subtotal    = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ── Business Methods ─────────────────────────────────────────────────────

    public void incrementQuantity(int amount) {
        this.quantity += amount;
        recalculate();
    }

    public void setQuantityAndRecalculate(int quantity) {
        this.quantity = quantity;
        recalculate();
    }

    private void recalculate() {
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
