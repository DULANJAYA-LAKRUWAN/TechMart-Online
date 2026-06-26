package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing a customer order.
 * Maps to the 'orders' table.
 *
 * Design Decisions:
 * - OrderStatus as @Enumerated(STRING) for readability in DB and reporting.
 * - totalAmount stored denormalized for performance (avoids SUM join on every lookup).
 * - @Version for optimistic locking during concurrent status transitions.
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_id",    columnList = "user_id"),
        @Index(name = "idx_orders_status",     columnList = "status"),
        @Index(name = "idx_orders_order_no",   columnList = "order_number", unique = true),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
    }
)
@NamedQueries({
    @NamedQuery(name = "Order.findByUser",
        query = "SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC"),
    @NamedQuery(name = "Order.findByStatus",
        query = "SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC"),
    @NamedQuery(name = "Order.findByOrderNumber",
        query = "SELECT o FROM Order o WHERE o.orderNumber = :orderNumber"),
    @NamedQuery(name = "Order.findRecentOrders",
        query = "SELECT o FROM Order o ORDER BY o.createdAt DESC"),
    @NamedQuery(name = "Order.countByStatus",
        query = "SELECT COUNT(o) FROM Order o WHERE o.status = :status"),
    @NamedQuery(name = "Order.sumRevenueToday",
        query = "SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.createdAt >= :startOfDay")
})
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Represents the lifecycle of an order */
    public enum OrderStatus {
        PENDING,        // placed but not yet processed
        PROCESSING,     // MDB is working on it
        CONFIRMED,      // payment validated, inventory reserved
        SHIPPED,        // dispatched from warehouse
        DELIVERED,      // received by customer
        COMPLETED,      // closed successfully
        CANCELLED,      // cancelled before dispatch
        REFUNDED        // refunded after completion
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(name = "order_seq", sequenceName = "orders_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @DecimalMin("0.0")
    @Column(name = "shipping_amount", precision = 10, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @DecimalMin("0.0")
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    /** ManyToOne: order belongs to one user */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** OneToMany: order has one or more items */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

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

    public Order() {}

    public Order(String orderNumber, User user, BigDecimal totalAmount) {
        this.orderNumber = orderNumber;
        this.user        = user;
        this.totalAmount = totalAmount;
    }

    // ── Business Methods ─────────────────────────────────────────────────────

    public void addItem(OrderItem item) {
        this.items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        this.items.remove(item);
        item.setOrder(null);
    }

    public void recalculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(shippingAmount != null ? shippingAmount : BigDecimal.ZERO)
            .add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
    }

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.PROCESSING;
    }

    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED || status == OrderStatus.DELIVERED;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return id != null && id.equals(order.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Order{id=" + id + ", orderNumber='" + orderNumber + "', status=" + status
            + ", total=" + totalAmount + "}";
    }
}
