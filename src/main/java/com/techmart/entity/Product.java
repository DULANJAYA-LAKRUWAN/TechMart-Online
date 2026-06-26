package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing a product in the TechMart catalog.
 * Maps to the 'products' table.
 *
 * Design Decisions:
 * - BigDecimal for price to avoid floating-point rounding errors (critical for financial data).
 * - @Version for optimistic locking — prevents overselling in concurrent orders.
 * - NamedQueries for JPA-level caching and query plan reuse.
 */
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_sku",      columnList = "sku", unique = true),
        @Index(name = "idx_products_category",  columnList = "category_id"),
        @Index(name = "idx_products_active",    columnList = "active"),
        @Index(name = "idx_products_price",     columnList = "price"),
        @Index(name = "idx_products_name",      columnList = "name")
    }
)
@NamedQueries({
    @NamedQuery(name = "Product.findAll",
        query = "SELECT p FROM Product p WHERE p.active = true ORDER BY p.name"),
    @NamedQuery(name = "Product.findBySku",
        query = "SELECT p FROM Product p WHERE p.sku = :sku AND p.active = true"),
    @NamedQuery(name = "Product.findByCategory",
        query = "SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.active = true ORDER BY p.name"),
    @NamedQuery(name = "Product.searchByName",
        query = "SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%',:name,'%')) AND p.active = true ORDER BY p.name"),
    @NamedQuery(name = "Product.findByPriceRange",
        query = "SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.active = true ORDER BY p.price"),
    @NamedQuery(name = "Product.countAll",
        query = "SELECT COUNT(p) FROM Product p WHERE p.active = true"),
    @NamedQuery(name = "Product.findFeatured",
        query = "SELECT p FROM Product p WHERE p.featured = true AND p.active = true ORDER BY p.createdAt DESC")
})
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "products_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(max = 50)
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @DecimalMin(value = "0.0")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "compare_price", precision = 12, scale = 2)
    private BigDecimal comparePrice;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "weight_kg", precision = 8, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "featured", nullable = false)
    private boolean featured = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    /** ManyToOne: many products belong to one category */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    /** OneToMany: a product can appear in many order items */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    /** OneToMany: inventory records across warehouses */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Inventory> inventoryRecords = new ArrayList<>();

    // ── Lifecycle Callbacks ──────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public Product() {}

    public Product(String name, String sku, BigDecimal price, Category category) {
        this.name     = name;
        this.sku      = sku;
        this.price    = price;
        this.category = category;
    }

    // ── Business Methods ─────────────────────────────────────────────────────

    public boolean isOnSale() {
        return comparePrice != null && comparePrice.compareTo(price) > 0;
    }

    public BigDecimal getDiscountAmount() {
        if (!isOnSale()) return BigDecimal.ZERO;
        return comparePrice.subtract(price);
    }

    public int getDiscountPercentage() {
        if (!isOnSale()) return 0;
        return comparePrice.subtract(price)
            .multiply(BigDecimal.valueOf(100))
            .divide(comparePrice, 0, java.math.RoundingMode.HALF_UP)
            .intValue();
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

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public List<Inventory> getInventoryRecords() { return inventoryRecords; }
    public void setInventoryRecords(List<Inventory> inventoryRecords) { this.inventoryRecords = inventoryRecords; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Product{id=" + id + ", sku='" + sku + "', name='" + name + "', price=" + price + "}";
    }
}
