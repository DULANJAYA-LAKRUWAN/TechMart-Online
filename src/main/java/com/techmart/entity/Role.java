package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity representing a security role (RBAC).
 * Maps to the 'roles' table.
 *
 * Standard roles: ADMIN, MANAGER, CUSTOMER, WAREHOUSE_STAFF
 */
@Entity
@Table(
    name = "roles",
    indexes = {
        @Index(name = "idx_roles_name", columnList = "name", unique = true)
    }
)
@NamedQueries({
    @NamedQuery(name = "Role.findByName",
        query = "SELECT r FROM Role r WHERE r.name = :name"),
    @NamedQuery(name = "Role.findAll",
        query = "SELECT r FROM Role r ORDER BY r.name")
})
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq")
    @SequenceGenerator(name = "role_seq", sequenceName = "roles_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    /** Owning side is User — mappedBy here */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    // ── Constructors ─────────────────────────────────────────────────────────

    public Role() {}

    public Role(String name, String description) {
        this.name        = name;
        this.description = description;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return id != null && id.equals(role.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Role{id=" + id + ", name='" + name + "'}";
    }
}
