package com.techmart.service;

import com.techmart.dto.UserDTO;

import javax.ejb.Local;
import java.util.List;
import java.util.Optional;

/**
 * Local EJB interface for User management operations.
 * Implemented by UserServiceBean (Stateless Session Bean).
 */
@Local
public interface UserService {

    /**
     * Register a new user in the system.
     * @param username  Unique username
     * @param email     Unique email address
     * @param password  Plain-text password (will be hashed)
     * @param firstName First name
     * @param lastName  Last name
     * @return          Created UserDTO
     */
    UserDTO registerUser(String username, String email, String password,
                         String firstName, String lastName);

    /**
     * Authenticate a user by username and password.
     * @return UserDTO if credentials are valid, throws exception otherwise
     */
    UserDTO authenticate(String username, String password);

    /**
     * Find a user by their ID.
     */
    Optional<UserDTO> findById(Long id);

    /**
     * Find a user by their email address.
     */
    Optional<UserDTO> findByEmail(String email);

    /**
     * Find a user by their username.
     */
    Optional<UserDTO> findByUsername(String username);

    /**
     * Retrieve all active users.
     */
    List<UserDTO> findAllUsers();

    /**
     * Update user profile information.
     */
    UserDTO updateUser(Long id, String firstName, String lastName, String phone);

    /**
     * Assign a role to a user.
     */
    void assignRole(Long userId, String roleName);

    /**
     * Remove a role from a user.
     */
    void removeRole(Long userId, String roleName);

    /**
     * Deactivate a user account (soft delete).
     */
    void deactivateUser(Long id);

    /**
     * Change a user's password.
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * Count the total number of active users.
     */
    long countActiveUsers();
}
