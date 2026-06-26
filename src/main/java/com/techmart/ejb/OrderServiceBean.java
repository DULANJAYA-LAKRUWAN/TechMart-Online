package com.techmart.ejb;

import com.techmart.cdi.Auditable;
import com.techmart.cdi.Monitored;
import com.techmart.cdi.OrderPlacedEvent;
import com.techmart.dto.OrderDTO;
import com.techmart.dto.OrderRequestDTO;
import com.techmart.entity.*;
import com.techmart.exception.InsufficientInventoryException;
import com.techmart.exception.OrderProcessingException;
import com.techmart.exception.ProductNotFoundException;
import com.techmart.jms.OrderQueueProducer;
import com.techmart.service.InventoryService;
import com.techmart.service.OrderService;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Stateless Session Bean — Order Management.
 *
 * Design Decisions:
 * - @Stateless: Order placement is stateless per request — cart data is passed in via DTO.
 * - @Asynchronous confirmOrderAsync(): decouples the REST response from payment processing.
 *   The client gets immediate acknowledgment; fulfillment happens asynchronously.
 * - JMS integration: OrderQueueProducer sends to OrderQueue for MDB processing.
 * - CDI Event: fires OrderPlacedEvent which OrderEventObserver handles for notifications.
 * - @EJB injection used for cross-EJB dependency.
 */
@Stateless
@Monitored
public class OrderServiceBean implements OrderService {

    private static final Logger LOG = Logger.getLogger(OrderServiceBean.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @Inject
    private OrderQueueProducer orderQueueProducer;

    @EJB
    private InventoryCacheBean inventoryCache;

    @Inject
    private Event<OrderPlacedEvent> orderPlacedEvent;

    // ── Place Order ──────────────────────────────────────────────────────────

    @Override
    @Auditable(action = "PLACE_ORDER", entity = "Order")
    public OrderDTO placeOrder(OrderRequestDTO request) {
        // 1. Load user
        User user = em.find(User.class, request.getUserId());
        if (user == null) {
            throw new OrderProcessingException("User not found: " + request.getUserId());
        }

        // 2. Validate and reserve inventory
        List<Object[]> lineItems = resolveLineItems(request);

        // 3. Build the order
        String orderNumber = generateOrderNumber();
        Order order = new Order(orderNumber, user, BigDecimal.ZERO);
        order.setShippingAddress(request.getShippingAddress());
        order.setBillingAddress(request.getBillingAddress() != null
            ? request.getBillingAddress() : request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());
        order.setShippingAmount(new BigDecimal("5.99"));
        order.setTaxAmount(BigDecimal.ZERO);

        for (Object[] lineItem : lineItems) {
            Product product = (Product) lineItem[0];
            int quantity    = (int) lineItem[1];
            OrderItem item  = new OrderItem(order, product, quantity, product.getPrice());
            order.addItem(item);
        }

        order.recalculateTotal();
        em.persist(order);
        em.flush();

        LOG.info("Order created: " + orderNumber);

        // 4. Send to JMS queue for async processing (payment + inventory deduction)
        String orderJson = buildOrderJson(order);
        orderQueueProducer.sendOrderForProcessing(orderNumber, orderJson);

        // 5. Fire CDI event for notification dispatch
        orderPlacedEvent.fire(new OrderPlacedEvent(
            order.getId(), orderNumber, user.getId(),
            user.getEmail(), order.getTotalAmount().doubleValue()));

        return toDTO(order);
    }

    // ── Async Confirmation ───────────────────────────────────────────────────

    @Override
    @Asynchronous
    @Auditable(action = "CONFIRM_ORDER_ASYNC", entity = "Order")
    public Future<OrderDTO> confirmOrderAsync(Long orderId) {
        try {
            Order order = em.find(Order.class, orderId);
            if (order == null) {
                throw new OrderProcessingException("Order not found: " + orderId);
            }
            // Simulate payment gateway processing delay
            Thread.sleep(500);

            order.setStatus(Order.OrderStatus.CONFIRMED);
            em.merge(order);
            LOG.info("Async order confirmed: " + order.getOrderNumber());
            return new AsyncResult<>(toDTO(order));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderProcessingException("Order confirmation interrupted: " + orderId);
        }
    }

    // ── Find Operations ──────────────────────────────────────────────────────

    @Override
    public Optional<OrderDTO> findById(Long id) {
        Order order = em.find(Order.class, id);
        return order != null ? Optional.of(toDTO(order)) : Optional.empty();
    }

    @Override
    public Optional<OrderDTO> findByOrderNumber(String orderNumber) {
        try {
            Order order = em.createNamedQuery("Order.findByOrderNumber", Order.class)
                .setParameter("orderNumber", orderNumber)
                .getSingleResult();
            return Optional.of(toDTO(order));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<OrderDTO> findOrdersByUser(Long userId) {
        return em.createNamedQuery("Order.findByUser", Order.class)
            .setParameter("userId", userId)
            .getResultList()
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> findOrdersByStatus(Order.OrderStatus status, int page, int size) {
        return em.createNamedQuery("Order.findByStatus", Order.class)
            .setParameter("status", status.name())
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList()
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> findAllOrders(int page, int size) {
        return em.createNamedQuery("Order.findRecentOrders", Order.class)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList()
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Cancel / Update ──────────────────────────────────────────────────────

    @Override
    @Auditable(action = "CANCEL_ORDER", entity = "Order")
    public OrderDTO cancelOrder(Long orderId, String reason) {
        Order order = em.find(Order.class, orderId);
        if (order == null) {
            throw new OrderProcessingException("Order not found: " + orderId);
        }
        if (!order.canBeCancelled()) {
            throw new OrderProcessingException(order.getOrderNumber(),
                "Order cannot be cancelled in status: " + order.getStatus());
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + "Cancelled: " + reason);
        em.merge(order);
        LOG.info("Order cancelled: " + order.getOrderNumber());
        return toDTO(order);
    }

    @Override
    @Auditable(action = "UPDATE_ORDER_STATUS", entity = "Order")
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = em.find(Order.class, orderId);
        if (order == null) throw new OrderProcessingException("Order not found: " + orderId);
        order.setStatus(newStatus);
        if (newStatus == Order.OrderStatus.SHIPPED) order.setShippedAt(LocalDateTime.now());
        if (newStatus == Order.OrderStatus.DELIVERED) order.setDeliveredAt(LocalDateTime.now());
        em.merge(order);
        return toDTO(order);
    }

    @Override
    public long countOrdersByStatus(Order.OrderStatus status) {
        return em.createNamedQuery("Order.countByStatus", Long.class)
            .setParameter("status", status.name())
            .getSingleResult();
    }

    @Override
    public BigDecimal calculateTodayRevenue() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Object result = em.createNamedQuery("Order.sumRevenueToday")
            .setParameter("startOfDay", startOfDay)
            .getSingleResult();
        return result != null ? (BigDecimal) result : BigDecimal.ZERO;
    }

    @Override
    public String generateOrderNumber() {
        return "TM-" + System.currentTimeMillis() + "-"
            + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private List<Object[]> resolveLineItems(OrderRequestDTO request) {
        return request.getItems().stream().map(itemReq -> {
            Product product = em.find(Product.class, itemReq.getProductId());
            if (product == null || !product.isActive()) {
                throw new ProductNotFoundException(itemReq.getProductId());
            }
            int available = inventoryCache.getAvailableStock(product.getId());
            if (available < itemReq.getQuantity()) {
                throw new InsufficientInventoryException(
                    product.getId(), itemReq.getQuantity(), available);
            }
            return new Object[]{product, itemReq.getQuantity()};
        }).collect(Collectors.toList());
    }

    private String buildOrderJson(Order order) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"orderId\":").append(order.getId()).append(",");
        sb.append("\"orderNumber\":\"").append(order.getOrderNumber()).append("\",");
        sb.append("\"userId\":").append(order.getUser().getId()).append(",");
        sb.append("\"total\":").append(order.getTotalAmount()).append(",");
        sb.append("\"itemCount\":").append(order.getItems().size());
        sb.append("}");
        return sb.toString();
    }

    private OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingAmount(order.getShippingAmount());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setUserFullName(order.getUser().getFullName());
        }
        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream().map(item -> {
                OrderDTO.OrderItemDTO i = new OrderDTO.OrderItemDTO();
                i.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                i.setProductName(item.getProductNameSnapshot());
                i.setProductSku(item.getProductSkuSnapshot());
                i.setQuantity(item.getQuantity());
                i.setUnitPrice(item.getUnitPrice());
                i.setSubtotal(item.getSubtotal());
                return i;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
