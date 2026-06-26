package com.techmart.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data Transfer Object for User — shields the entity from direct exposure in REST/JSF layers.
 * Password hash is intentionally excluded.
 */
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean active;
    private LocalDateTime createdAt;
    private Set<String> roles;

    // ── Constructors ─────────────────────────────────────────────────────────

    public UserDTO() {}

    public UserDTO(Long id, String username, String email,
                   String firstName, String lastName, boolean active) {
        this.id        = id;
        this.username  = username;
        this.email     = email;
        this.firstName = firstName;
        this.lastName  = lastName;
        this.active    = active;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return firstName + " " + lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}
