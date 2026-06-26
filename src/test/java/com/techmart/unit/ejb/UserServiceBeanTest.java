package com.techmart.unit.ejb;

import com.techmart.dto.UserDTO;
import com.techmart.ejb.UserServiceBean;
import com.techmart.entity.Role;
import com.techmart.entity.User;
import com.techmart.exception.TechMartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceBeanTest {

    @Mock private EntityManager em;
    @Mock private TypedQuery<User> usernameQuery;
    @Mock private TypedQuery<User> emailQuery;
    @Mock private TypedQuery<Role> roleQuery;
    @Mock private TypedQuery<Long> countQuery;

    @InjectMocks
    private UserServiceBean userService;

    private User testUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = new Role("CUSTOMER", "Standard customer role");
        customerRole.setId(1L);

        testUser = new User("johndoe", "john@example.com",
                "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
                "John", "Doe");
        testUser.setId(1L);
        testUser.addRole(customerRole);
    }

    @Test
    void registerUser_shouldSucceed_whenUsernameAndEmailAreUnique() {
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(usernameQuery);
        when(usernameQuery.setParameter("username", "newuser")).thenReturn(usernameQuery);
        when(usernameQuery.getResultList()).thenReturn(Collections.emptyList());

        when(em.createNamedQuery("User.findByEmail", User.class)).thenReturn(emailQuery);
        when(emailQuery.setParameter("email", "new@example.com")).thenReturn(emailQuery);
        when(emailQuery.getResultList()).thenReturn(Collections.emptyList());

        when(em.createNamedQuery("Role.findByName", Role.class)).thenReturn(roleQuery);
        when(roleQuery.setParameter("name", "CUSTOMER")).thenReturn(roleQuery);
        when(roleQuery.getSingleResult()).thenReturn(customerRole);

        UserDTO result = userService.registerUser("newuser", "new@example.com", "password",
                "New", "User");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertTrue(result.getRoles().contains("CUSTOMER"));
        verify(em).persist(any(User.class));
        verify(em).flush();
    }

    @Test
    void registerUser_shouldThrow_whenUsernameTaken() {
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(usernameQuery);
        when(usernameQuery.setParameter("username", "johndoe")).thenReturn(usernameQuery);
        when(usernameQuery.getResultList()).thenReturn(Collections.singletonList(testUser));

        assertThrows(TechMartException.class,
            () -> userService.registerUser("johndoe", "other@example.com", "pass", "J", "D"));
    }

    @Test
    void registerUser_shouldThrow_whenEmailTaken() {
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(usernameQuery);
        when(usernameQuery.setParameter("username", "unique_user")).thenReturn(usernameQuery);
        when(usernameQuery.getResultList()).thenReturn(Collections.emptyList());

        when(em.createNamedQuery("User.findByEmail", User.class)).thenReturn(emailQuery);
        when(emailQuery.setParameter("email", "john@example.com")).thenReturn(emailQuery);
        when(emailQuery.getResultList()).thenReturn(Collections.singletonList(testUser));

        assertThrows(TechMartException.class,
            () -> userService.registerUser("unique_user", "john@example.com", "pass", "J", "D"));
    }

    @Test
    void authenticate_shouldSucceed_withCorrectPassword() {
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(usernameQuery);
        when(usernameQuery.setParameter("username", "johndoe")).thenReturn(usernameQuery);
        when(usernameQuery.getSingleResult()).thenReturn(testUser);

        UserDTO result = userService.authenticate("johndoe", "password");

        assertNotNull(result);
        assertEquals("johndoe", result.getUsername());
    }

    @Test
    void authenticate_shouldThrow_withWrongPassword() {
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(usernameQuery);
        when(usernameQuery.setParameter("username", "johndoe")).thenReturn(usernameQuery);
        when(usernameQuery.getSingleResult()).thenReturn(testUser);

        assertThrows(TechMartException.class,
            () -> userService.authenticate("johndoe", "wrongpassword"));
    }

    @Test
    void authenticate_shouldThrow_whenUserNotFound() {
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(usernameQuery);
        when(usernameQuery.setParameter("username", "unknown")).thenReturn(usernameQuery);
        when(usernameQuery.getSingleResult()).thenThrow(new NoResultException());

        assertThrows(TechMartException.class,
            () -> userService.authenticate("unknown", "password"));
    }

    @Test
    void findById_shouldReturnUser_whenExistsAndActive() {
        when(em.find(User.class, 1L)).thenReturn(testUser);

        Optional<UserDTO> result = userService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("johndoe", result.get().getUsername());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        when(em.find(User.class, 999L)).thenReturn(null);

        Optional<UserDTO> result = userService.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void findById_shouldReturnEmpty_whenInactive() {
        testUser.deactivate();
        when(em.find(User.class, 1L)).thenReturn(testUser);

        Optional<UserDTO> result = userService.findById(1L);

        assertFalse(result.isPresent());
    }

    @Test
    void updateUser_shouldModifyAndReturn() {
        when(em.find(User.class, 1L)).thenReturn(testUser);
        when(em.merge(testUser)).thenReturn(testUser);

        UserDTO result = userService.updateUser(1L, "Jonathan", "Doe", "555-0100");

        assertEquals("Jonathan", result.getFirstName());
        assertEquals("555-0100", result.getPhone());
        verify(em).merge(testUser);
    }

    @Test
    void changePassword_shouldSucceed_withCorrectOldPassword() {
        when(em.find(User.class, 1L)).thenReturn(testUser);

        userService.changePassword(1L, "password", "newpassword");

        verify(em).merge(testUser);
        assertNotEquals("password_hash", testUser.getPasswordHash());
    }

    @Test
    void changePassword_shouldThrow_withWrongOldPassword() {
        when(em.find(User.class, 1L)).thenReturn(testUser);

        assertThrows(TechMartException.class,
            () -> userService.changePassword(1L, "wrong_old", "newpassword"));
    }

    @Test
    void assignRole_shouldAddRole_whenFound() {
        Role adminRole = new Role("ADMIN", "Admin role");
        adminRole.setId(2L);
        when(em.find(User.class, 1L)).thenReturn(testUser);
        when(em.createNamedQuery("Role.findByName", Role.class)).thenReturn(roleQuery);
        when(roleQuery.setParameter("name", "ADMIN")).thenReturn(roleQuery);
        when(roleQuery.getSingleResult()).thenReturn(adminRole);

        userService.assignRole(1L, "ADMIN");

        assertTrue(testUser.hasRole("ADMIN"));
        verify(em).merge(testUser);
    }

    @Test
    void assignRole_shouldThrow_whenRoleNotFound() {
        when(em.find(User.class, 1L)).thenReturn(testUser);
        when(em.createNamedQuery("Role.findByName", Role.class)).thenReturn(roleQuery);
        when(roleQuery.setParameter("name", "NONEXISTENT")).thenReturn(roleQuery);
        when(roleQuery.getSingleResult()).thenThrow(new NoResultException());

        assertThrows(TechMartException.class, () -> userService.assignRole(1L, "NONEXISTENT"));
    }

    @Test
    void deactivateUser_shouldSetInactive() {
        when(em.find(User.class, 1L)).thenReturn(testUser);

        userService.deactivateUser(1L);

        assertFalse(testUser.isActive());
        verify(em).merge(testUser);
    }

    @Test
    void countActiveUsers_shouldReturnCount() {
        when(em.createNamedQuery("User.countActive", Long.class)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(10L);

        long count = userService.countActiveUsers();

        assertEquals(10L, count);
    }

    @Test
    void findByEmail_shouldReturnUser_whenFound() {
        when(em.createNamedQuery("User.findByEmail", User.class)).thenReturn(emailQuery);
        when(emailQuery.setParameter("email", "john@example.com")).thenReturn(emailQuery);
        when(emailQuery.getSingleResult()).thenReturn(testUser);

        Optional<UserDTO> result = userService.findByEmail("john@example.com");

        assertTrue(result.isPresent());
        assertEquals("johndoe", result.get().getUsername());
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenNotFound() {
        when(em.createNamedQuery("User.findByEmail", User.class)).thenReturn(emailQuery);
        when(emailQuery.setParameter("email", "unknown@example.com")).thenReturn(emailQuery);
        when(emailQuery.getSingleResult()).thenThrow(new NoResultException());

        Optional<UserDTO> result = userService.findByEmail("unknown@example.com");

        assertFalse(result.isPresent());
    }
}
