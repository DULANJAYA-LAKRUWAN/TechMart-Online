package com.techmart.rest;

import com.techmart.dto.ApiResponseDTO;
import com.techmart.dto.OrderDTO;
import com.techmart.dto.OrderRequestDTO;
import com.techmart.entity.Order;
import com.techmart.service.OrderService;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JAX-RS REST Resource — Order Management API.
 *
 * Endpoints:
 *   POST   /api/orders              → Place new order
 *   GET    /api/orders              → All orders (paginated, admin)
 *   GET    /api/orders/{id}         → Order by ID
 *   GET    /api/orders/number/{no}  → Order by order number
 *   GET    /api/orders/user/{uid}   → Orders by user
 *   GET    /api/orders/status/{s}   → Orders by status
 *   PUT    /api/orders/{id}/cancel  → Cancel order
 *   PUT    /api/orders/{id}/status  → Update status (admin)
 *   GET    /api/orders/revenue      → Today's revenue
 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @EJB
    private OrderService orderService;

    @POST
    public Response placeOrder(OrderRequestDTO request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponseDTO.error("Order must contain at least one item", "EMPTY_ORDER"))
                .build();
        }
        OrderDTO created = orderService.placeOrder(request);
        return Response.status(Response.Status.CREATED)
            .entity(ApiResponseDTO.success(created,
                "Order placed successfully. Order #: " + created.getOrderNumber()))
            .build();
    }

    @GET
    public Response getAllOrders(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        List<OrderDTO> orders = orderService.findAllOrders(page, size);
        return Response.ok(ApiResponseDTO.success(orders, "Orders retrieved", orders.size())).build();
    }

    @GET
    @Path("/{id}")
    public Response getOrderById(@PathParam("id") Long id) {
        Optional<OrderDTO> order = orderService.findById(id);
        return order.map(o -> Response.ok(ApiResponseDTO.success(o)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponseDTO.error("Order not found: " + id, "ORDER_NOT_FOUND"))
                .build());
    }

    @GET
    @Path("/number/{orderNumber}")
    public Response getOrderByNumber(@PathParam("orderNumber") String orderNumber) {
        Optional<OrderDTO> order = orderService.findByOrderNumber(orderNumber);
        return order.map(o -> Response.ok(ApiResponseDTO.success(o)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponseDTO.error("Order not found: " + orderNumber, "ORDER_NOT_FOUND"))
                .build());
    }

    @GET
    @Path("/user/{userId}")
    public Response getOrdersByUser(@PathParam("userId") Long userId) {
        List<OrderDTO> orders = orderService.findOrdersByUser(userId);
        return Response.ok(ApiResponseDTO.success(orders, "User orders", orders.size())).build();
    }

    @GET
    @Path("/status/{status}")
    public Response getOrdersByStatus(
            @PathParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            List<OrderDTO> orders = orderService.findOrdersByStatus(orderStatus, page, size);
            return Response.ok(ApiResponseDTO.success(orders, "Orders with status: " + status, orders.size())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponseDTO.error("Invalid status: " + status, "INVALID_STATUS"))
                .build();
        }
    }

    @PUT
    @Path("/{id}/cancel")
    public Response cancelOrder(@PathParam("id") Long id, Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Customer requested cancellation") : "";
        OrderDTO cancelled = orderService.cancelOrder(id, reason);
        return Response.ok(ApiResponseDTO.success(cancelled, "Order cancelled")).build();
    }

    @PUT
    @Path("/{id}/status")
    public Response updateOrderStatus(@PathParam("id") Long id, Map<String, String> body) {
        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(body.get("status").toUpperCase());
            OrderDTO updated = orderService.updateOrderStatus(id, newStatus);
            return Response.ok(ApiResponseDTO.success(updated, "Order status updated")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponseDTO.error("Invalid status", "INVALID_STATUS"))
                .build();
        }
    }

    @GET
    @Path("/revenue/today")
    public Response getTodayRevenue() {
        BigDecimal revenue = orderService.calculateTodayRevenue();
        return Response.ok(ApiResponseDTO.success(revenue, "Today's revenue")).build();
    }

    @GET
    @Path("/count/{status}")
    public Response countByStatus(@PathParam("status") String status) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        long count = orderService.countOrdersByStatus(orderStatus);
        return Response.ok(ApiResponseDTO.success(count, "Order count for: " + status)).build();
    }
}
