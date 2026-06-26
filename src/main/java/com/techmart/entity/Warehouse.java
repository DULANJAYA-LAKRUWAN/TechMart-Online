package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing a physical warehouse location.
 * Maps to the 'warehouses' table.
 */
@Entity
@Table(
    name = "warehouses",
    indexes = {
        @Index(name = "idx_warehouses_code",   columnList = "code", unique = true),
        @Index(name = "idx_warehouses_active",  columnList = "active"),
        @Index(name = "idx_warehouses_country", columnList = "country")
    }
)
@NamedQueries({
    @NamedQuery(name = "Warehouse.findAll",
        query = "SELECT w FROM Warehouse w WHERE w.active = true ORDER BY w.name"),
    @NamedQuery(name = "Warehouse.findByCode",
        query = "SELECT w FROM Warehouse w WHERE w.code = :code AND w.active = true"),
    @NamedQuery(name = "Warehouse.findByCountry",
        query = "SELECT w FROM Warehouse w WHERE w.country = :country AND w.active = true ORDER BY w.name")
})
public class Warehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "warehouse_seq")
    @SequenceGenerator(name = "warehouse_seq", sequenceName = "warehouses_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 20)
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank
    @Size(max = 255)
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @NotBlank
    @Size(max = 100)
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank
    @Size(max = 100)
    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Size(max = 20)
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Inventory> inventoryRecords = new ArrayList<>();

    // ── Lifecycle Callbacks ──────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public Warehouse() {}

    public Warehouse(String name, String code, String address, String city, String country) {
        this.name    = name;
        this.code    = code;
        this.address = address;
        this.city    = city;
        this.country = country;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<Inventory> getInventoryRecords() { return inventoryRecords; }
    public void setInventoryRecords(List<Inventory> inventoryRecords) { this.inventoryRecords = inventoryRecords; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Warehouse)) return false;
        Warehouse warehouse = (Warehouse) o;
        return id != null && id.equals(warehouse.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Warehouse{id=" + id + ", code='" + code + "', name='" + name + "'}";
    }
}
