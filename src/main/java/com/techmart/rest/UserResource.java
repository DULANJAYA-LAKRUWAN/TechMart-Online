package com.techmart.rest;

import com.techmart.dto.ApiResponseDTO;
import com.techmart.dto.UserDTO;
import com.techmart.service.UserService;

import javax.ejb.EJB;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JAX-RS REST Resource — User Management API.
 *
 * Endpoints:
 *   GET    /api/users              → List all active users
 *   GET    /api/users/{id}         → Get user by ID
 *   GET    /api/users/email/{email}→ Get user by email
 *   POST   /api/users/register     → Register new user
 *   POST   /api/users/authenticate → Authenticate user
 *   PUT    /api/users/{id}         → Update user profile
 *   POST   /api/users/{id}/roles   → Assign role
 *   DELETE /api/users/{id}         → Deactivate user
 *   GET    /api/users/count        → Count active users
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @EJB
    private UserService userService;

    @GET
    public Response getAllUsers() {
        List<UserDTO> users = userService.findAllUsers();
        return Response.ok(
            ApiResponseDTO.success(users, "Users retrieved", users.size())).build();
    }

    @GET
    @Path("/{id}")
    public Response getUserById(@PathParam("id") Long id) {
        Optional<UserDTO> user = userService.findById(id);
        return user.map(u -> Response.ok(ApiResponseDTO.success(u)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponseDTO.error("User not found: " + id, "USER_NOT_FOUND"))
                .build());
    }

    @GET
    @Path("/email/{email}")
    public Response getUserByEmail(@PathParam("email") String email) {
        Optional<UserDTO> user = userService.findByEmail(email);
        return user.map(u -> Response.ok(ApiResponseDTO.success(u)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponseDTO.error("User not found with email: " + email, "USER_NOT_FOUND"))
                .build());
    }

    @POST
    @Path("/register")
    public Response registerUser(Map<String, String> body) {
        String username  = body.get("username");
        String email     = body.get("email");
        String password  = body.get("password");
        String firstName = body.get("firstName");
        String lastName  = body.get("lastName");

        if (username == null || email == null || password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponseDTO.error("username, email, and password are required", "MISSING_FIELDS"))
                .build();
        }

        UserDTO created = userService.registerUser(username, email, password, firstName, lastName);
        return Response.status(Response.Status.CREATED)
            .entity(ApiResponseDTO.success(created, "User registered successfully"))
            .build();
    }

    @POST
    @Path("/authenticate")
    public Response authenticate(Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        UserDTO user = userService.authenticate(username, password);
        return Response.ok(ApiResponseDTO.success(user, "Authentication successful")).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateUser(@PathParam("id") Long id, Map<String, String> body) {
        UserDTO updated = userService.updateUser(
            id, body.get("firstName"), body.get("lastName"), body.get("phone"));
        return Response.ok(ApiResponseDTO.success(updated, "User updated")).build();
    }

    @POST
    @Path("/{id}/roles")
    public Response assignRole(@PathParam("id") Long userId, Map<String, String> body) {
        userService.assignRole(userId, body.get("role"));
        return Response.ok(ApiResponseDTO.success(null, "Role assigned")).build();
    }

    @DELETE
    @Path("/{id}/roles/{role}")
    public Response removeRole(@PathParam("id") Long userId, @PathParam("role") String role) {
        userService.removeRole(userId, role);
        return Response.ok(ApiResponseDTO.success(null, "Role removed")).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deactivateUser(@PathParam("id") Long id) {
        userService.deactivateUser(id);
        return Response.ok(ApiResponseDTO.success(null, "User deactivated")).build();
    }

    @POST
    @Path("/{id}/change-password")
    public Response changePassword(@PathParam("id") Long id, Map<String, String> body) {
        userService.changePassword(id, body.get("oldPassword"), body.get("newPassword"));
        return Response.ok(ApiResponseDTO.success(null, "Password changed")).build();
    }

    @GET
    @Path("/count")
    public Response countUsers() {
        long count = userService.countActiveUsers();
        return Response.ok(ApiResponseDTO.success(count, "Active user count")).build();
    }
}
