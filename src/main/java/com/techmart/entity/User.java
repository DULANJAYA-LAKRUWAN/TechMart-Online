package com.techmart.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity representing a platform user.
 * Maps to the 'users' table in PostgreSQL.
 *
 * Design Decisions:
 * - ManyToMany with Role enables fine-grained RBAC.
 * - @Version enables optimistic locking to handle concurrent updates safely.
 * - Passwords stored as BCrypt hash (never plain text).
 * - Soft-delete via 'active' flag instead of physical deletion to preserve audit trails.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_username", columnList = "username", unique = true),
        @Index(name = "idx_users_active", columnList = "active")
    }
)
@NamedQueries({
    @NamedQuery(name = "User.findByEmail",
        query = "SELECT u FROM User u WHERE u.email = :email AND u.active = true"),
    @NamedQuery(name = "User.findByUsername",
        query = "SELECT u FROM User u WHERE u.username = :username AND u.active = true"),
    @NamedQuery(name = "User.findAllActive",
        query = "SELECT u FROM User u WHERE u.active = true ORDER BY u.createdAt DESC"),
    @NamedQuery(name = "User.countActive",
        query = "SELECT COUNT(u) FROM User u WHERE u.active = true")
})
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "users_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password hash is required")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    /** Many-to-Many: one user can have multiple roles */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

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

    public User() {}

    public User(String username, String email, String passwordHash,
                String firstName, String lastName) {
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.firstName    = firstName;
        this.lastName     = lastName;
    }

    // ── Business Methods ─────────────────────────────────────────────────────

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }

    public void deactivate() {
        this.active = false;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }
}
