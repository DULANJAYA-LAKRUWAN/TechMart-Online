package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing a product category.
 * Supports a self-referencing parent-child hierarchy (e.g. Electronics > Laptops).
 * Maps to the 'categories' table.
 */
@Entity
@Table(
    name = "categories",
    indexes = {
        @Index(name = "idx_categories_name", columnList = "name"),
        @Index(name = "idx_categories_parent_id", columnList = "parent_id"),
        @Index(name = "idx_categories_active", columnList = "active")
    }
)
@NamedQueries({
    @NamedQuery(name = "Category.findAllActive",
        query = "SELECT c FROM Category c WHERE c.active = true ORDER BY c.name"),
    @NamedQuery(name = "Category.findRootCategories",
        query = "SELECT c FROM Category c WHERE c.parent IS NULL AND c.active = true ORDER BY c.name"),
    @NamedQuery(name = "Category.findByName",
        query = "SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) AND c.active = true")
})
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_seq")
    @SequenceGenerator(name = "category_seq", sequenceName = "categories_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Self-referencing parent category for hierarchy */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    private Category parent;

    /** Child categories */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> children = new ArrayList<>();

    /** Products in this category */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

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

    public Category() {}

    public Category(String name, String description) {
        this.name        = name;
        this.description = description;
    }

    // ── Business Methods ─────────────────────────────────────────────────────

    public boolean isRootCategory() {
        return this.parent == null;
    }

    public int getProductCount() {
        return products != null ? products.size() : 0;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Category getParent() { return parent; }
    public void setParent(Category parent) { this.parent = parent; }

    public List<Category> getChildren() { return children; }
    public void setChildren(List<Category> children) { this.children = children; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return id != null && id.equals(category.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "'}";
    }
}
