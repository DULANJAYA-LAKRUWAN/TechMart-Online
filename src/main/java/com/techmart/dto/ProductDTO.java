package com.techmart.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Data Transfer Object for Product catalog display.
 */
public class ProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String sku;
    private String description;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private String imageUrl;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private boolean featured;
    private boolean active;
    private boolean onSale;
    private int discountPercentage;

    // ── Constructors ─────────────────────────────────────────────────────────

    public ProductDTO() {}

    public ProductDTO(Long id, String name, String sku, BigDecimal price) {
        this.id    = id;
        this.name  = name;
        this.sku   = sku;
        this.price = price;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getComparePrice() { return comparePrice; }
    public void setComparePrice(BigDecimal comparePrice) { this.comparePrice = comparePrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isOnSale() { return onSale; }
    public void setOnSale(boolean onSale) { this.onSale = onSale; }

    public int getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(int discountPercentage) { this.discountPercentage = discountPercentage; }
}
