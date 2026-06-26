package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * JPA Entity representing a line item within a customer order.
 * Maps to the 'order_items' table.
 *
 * Design Decisions:
 * - Unit price snapshotted at order time — prevents price changes from affecting historical orders.
 * - Subtotal calculated and stored for reporting performance.
 */
@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_items_order_id",   columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
    }
)
@NamedQueries({
    @NamedQuery(name = "OrderItem.findByOrder",
        query = "SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId"),
    @NamedQuery(name = "OrderItem.findTopSellingProducts",
        query = "SELECT oi.product.id, SUM(oi.quantity) AS total " +
                "FROM OrderItem oi GROUP BY oi.product.id ORDER BY total DESC")
})
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_seq")
    @SequenceGenerator(name = "order_item_seq", sequenceName = "order_items_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "product_name_snapshot", length = 200)
    private String productNameSnapshot;

    @Column(name = "product_sku_snapshot", length = 50)
    private String productSkuSnapshot;

    /** ManyToOne: item belongs to one order */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** ManyToOne: item references one product */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ── Constructors ─────────────────────────────────────────────────────────

    public OrderItem() {}

    public OrderItem(Order order, Product product, int quantity, BigDecimal unitPrice) {
        this.order               = order;
        this.product             = product;
        this.quantity            = quantity;
        this.unitPrice           = unitPrice;
        this.subtotal            = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.productNameSnapshot = product.getName();
        this.productSkuSnapshot  = product.getSku();
    }

    // ── Business Methods ─────────────────────────────────────────────────────

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public void recalculate() {
        this.subtotal = getSubtotal();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        recalculate();
    }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        recalculate();
    }

    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }

    public String getProductSkuSnapshot() { return productSkuSnapshot; }
    public void setProductSkuSnapshot(String productSkuSnapshot) { this.productSkuSnapshot = productSkuSnapshot; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        OrderItem orderItem = (OrderItem) o;
        return id != null && id.equals(orderItem.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "OrderItem{id=" + id + ", productSku='" + productSkuSnapshot
            + "', qty=" + quantity + ", subtotal=" + subtotal + "}";
    }
}
