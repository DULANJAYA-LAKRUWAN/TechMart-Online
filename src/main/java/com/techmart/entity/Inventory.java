package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA Entity representing product stock levels at a specific warehouse.
 * Maps to the 'inventory' table.
 *
 * Design Decisions:
 * - Composite unique constraint (product + warehouse) enforces one inventory record per location.
 * - @Version for optimistic locking — critical for preventing race conditions during stock updates.
 * - Threshold-based low-stock alert via business method.
 */
@Entity
@Table(
    name = "inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_inventory_product_warehouse",
            columnNames = {"product_id", "warehouse_id"})
    },
    indexes = {
        @Index(name = "idx_inventory_product",    columnList = "product_id"),
        @Index(name = "idx_inventory_warehouse",  columnList = "warehouse_id"),
        @Index(name = "idx_inventory_quantity",   columnList = "quantity_in_stock")
    }
)
@NamedQueries({
    @NamedQuery(name = "Inventory.findByProduct",
        query = "SELECT i FROM Inventory i WHERE i.product.id = :productId"),
    @NamedQuery(name = "Inventory.findByWarehouse",
        query = "SELECT i FROM Inventory i WHERE i.warehouse.id = :warehouseId ORDER BY i.product.name"),
    @NamedQuery(name = "Inventory.findTotalStockByProduct",
        query = "SELECT SUM(i.quantityInStock) FROM Inventory i WHERE i.product.id = :productId"),
    @NamedQuery(name = "Inventory.findLowStock",
        query = "SELECT i FROM Inventory i WHERE i.quantityInStock <= i.reorderThreshold ORDER BY i.quantityInStock ASC"),
    @NamedQuery(name = "Inventory.findByProductAndWarehouse",
        query = "SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId")
})
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_seq")
    @SequenceGenerator(name = "inventory_seq", sequenceName = "inventory_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock = 0;

    @Min(value = 0)
    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;

    @Min(value = 0)
    @Column(name = "reorder_threshold", nullable = false)
    private Integer reorderThreshold = 10;

    @Min(value = 0)
    @Column(name = "reorder_quantity")
    private Integer reorderQuantity = 50;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    // ── Lifecycle Callbacks ──────────────────────────────────────────────────

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public Inventory() {}

    public Inventory(Product product, Warehouse warehouse, int quantityInStock) {
        this.product         = product;
        this.warehouse       = warehouse;
        this.quantityInStock = quantityInStock;
    }

    // ── Business Methods ─────────────────────────────────────────────────────

    /** Returns quantity available for new orders (in stock minus reserved) */
    public int getAvailableQuantity() {
        return Math.max(0, quantityInStock - quantityReserved);
    }

    /** Returns true if stock is at or below the reorder threshold */
    public boolean isLowStock() {
        return quantityInStock <= reorderThreshold;
    }

    /** Reserve stock for an in-progress order */
    public void reserve(int quantity) {
        if (quantity > getAvailableQuantity()) {
            throw new IllegalStateException("Insufficient available stock. Available: "
                + getAvailableQuantity() + ", Requested: " + quantity);
        }
        this.quantityReserved += quantity;
    }

    /** Confirm stock deduction after order completion */
    public void deduct(int quantity) {
        if (quantity > this.quantityInStock) {
            throw new IllegalStateException("Cannot deduct more than in stock.");
        }
        this.quantityInStock  -= quantity;
        this.quantityReserved  = Math.max(0, this.quantityReserved - quantity);
    }

    /** Release reservation without deduction (e.g. cancelled order) */
    public void releaseReservation(int quantity) {
        this.quantityReserved = Math.max(0, this.quantityReserved - quantity);
    }

    /** Restock the warehouse with new supply */
    public void restock(int quantity) {
        this.quantityInStock += quantity;
        this.lastRestockedAt  = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getQuantityInStock() { return quantityInStock; }
    public void setQuantityInStock(Integer quantityInStock) { this.quantityInStock = quantityInStock; }

    public Integer getQuantityReserved() { return quantityReserved; }
    public void setQuantityReserved(Integer quantityReserved) { this.quantityReserved = quantityReserved; }

    public Integer getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(Integer reorderThreshold) { this.reorderThreshold = reorderThreshold; }

    public Integer getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(Integer reorderQuantity) { this.reorderQuantity = reorderQuantity; }

    public LocalDateTime getLastRestockedAt() { return lastRestockedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inventory)) return false;
        Inventory inventory = (Inventory) o;
        return id != null && id.equals(inventory.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Inventory{id=" + id + ", productId=" + (product != null ? product.getId() : null)
            + ", warehouseId=" + (warehouse != null ? warehouse.getId() : null)
            + ", qty=" + quantityInStock + "}";
    }
}
