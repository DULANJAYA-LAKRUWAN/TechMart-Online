package com.techmart.rest;

import com.techmart.dto.ApiResponseDTO;
import com.techmart.dto.NotificationDTO;
import com.techmart.entity.Notification;
import com.techmart.service.NotificationService;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JAX-RS REST Resource — Notification API.
 *
 * Endpoints:
 *   GET    /api/notifications/{userId}          → List notifications for user
 *   PATCH  /api/notifications/{id}/read        → Mark single notification as read
 *   PATCH  /api/notifications/user/{userId}/read → Mark all as read for user
 *   GET    /api/notifications/unread/count/{userId} → Unread count
 */
@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @EJB
    private NotificationService notificationService;

    @GET
    @Path("/user/{userId}")
    public Response getUserNotifications(@PathParam("userId") Long userId) {
        List<Notification> list = notificationService.findByUser(userId);
        List<NotificationDTO> dtos = list.stream()
            .map(NotificationDTO::fromEntity)
            .collect(Collectors.toList());
        return Response.ok(ApiResponseDTO.success(dtos, "Notifications retrieved"))
            .build();
    }

    @PATCH
    @Path("/{id}/read")
    public Response markAsRead(@PathParam("id") Long notificationId) {
        notificationService.markAsRead(notificationId);
        return Response.ok(ApiResponseDTO.success(null, "Notification marked as read"))
            .build();
    }

    @PATCH
    @Path("/user/{userId}/read")
    public Response markAllAsRead(@PathParam("userId") Long userId) {
        notificationService.markAllAsRead(userId);
        return Response.ok(ApiResponseDTO.success(null, "All notifications marked as read"))
            .build();
    }

    @GET
    @Path("/unread/count/{userId}")
    public Response unreadCount(@PathParam("userId") Long userId) {
        long count = notificationService.countUnread(userId);
        return Response.ok(ApiResponseDTO.success(count, "Unread count"))
            .build();
    }
}
