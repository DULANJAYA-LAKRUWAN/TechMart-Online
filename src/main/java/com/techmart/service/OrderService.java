package com.techmart.service;

import com.techmart.dto.OrderDTO;
import com.techmart.dto.OrderRequestDTO;
import com.techmart.entity.Order;

import javax.ejb.Local;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Local EJB interface for Order management.
 * Implemented by OrderServiceBean (Stateless Session Bean).
 */
@Local
public interface OrderService {

    /**
     * Place a new order synchronously.
     * Creates the order record and dispatches async processing via JMS.
     * @return Created order DTO
     */
    OrderDTO placeOrder(OrderRequestDTO request);

    /**
     * Asynchronously confirm an order after payment validation.
     * Returns immediately; actual work happens in background thread.
     */
    Future<OrderDTO> confirmOrderAsync(Long orderId);

    /**
     * Find an order by its internal ID.
     */
    Optional<OrderDTO> findById(Long id);

    /**
     * Find an order by its human-readable order number.
     */
    Optional<OrderDTO> findByOrderNumber(String orderNumber);

    /**
     * Retrieve all orders for a specific user.
     */
    List<OrderDTO> findOrdersByUser(Long userId);

    /**
     * Retrieve orders by status with pagination.
     */
    List<OrderDTO> findOrdersByStatus(Order.OrderStatus status, int page, int size);

    /**
     * Retrieve all recent orders with pagination.
     */
    List<OrderDTO> findAllOrders(int page, int size);

    /**
     * Cancel an order (only possible for PENDING or PROCESSING orders).
     */
    OrderDTO cancelOrder(Long orderId, String reason);

    /**
     * Update order status (admin operation).
     */
    OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus newStatus);

    /**
     * Count orders by status.
     */
    long countOrdersByStatus(Order.OrderStatus status);

    /**
     * Calculate total revenue for today.
     */
    BigDecimal calculateTodayRevenue();

    /**
     * Generate a unique order number with prefix TM- and timestamp.
     */
    String generateOrderNumber();
}
