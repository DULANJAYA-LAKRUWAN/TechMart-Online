package com.techmart.unit.ejb;

import com.techmart.cdi.OrderPlacedEvent;
import com.techmart.dto.OrderDTO;
import com.techmart.dto.OrderRequestDTO;
import com.techmart.dto.OrderRequestDTO.OrderItemRequest;
import com.techmart.ejb.InventoryCacheBean;
import com.techmart.ejb.OrderServiceBean;
import com.techmart.entity.*;
import com.techmart.exception.*;
import com.techmart.jms.OrderQueueProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceBeanTest {

    @Mock private EntityManager em;
    @Mock private OrderQueueProducer orderQueueProducer;
    @Mock private InventoryCacheBean inventoryCache;
    @Mock private Event<OrderPlacedEvent> orderPlacedEvent;
    @Mock private TypedQuery<Order> typedQuery;

    @InjectMocks
    private OrderServiceBean orderService;

    private User testUser;
    private Product testProduct;
    private Order testOrder;
    private OrderRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("johndoe", "john@example.com", "hash", "John", "Doe");
        testUser.setId(1L);

        Category cat = new Category("Electronics", "Electronic items");
        cat.setId(10L);
        testProduct = new Product("Laptop", "LAP-001", new BigDecimal("999.99"), cat);
        testProduct.setId(100L);

        testOrder = new Order("TM-123-ABC", testUser, BigDecimal.ZERO);
        testOrder.setId(1L);
        testOrder.setShippingAddress("123 Main St");
        testOrder.setPaymentMethod("CREDIT_CARD");

        validRequest = new OrderRequestDTO();
        validRequest.setUserId(1L);
        validRequest.setShippingAddress("123 Main St");
        validRequest.setPaymentMethod("CREDIT_CARD");
        validRequest.setItems(Collections.singletonList(new OrderItemRequest(100L, 2)));
    }

    @Test
    void placeOrder_shouldSucceed_whenAllValidationsPass() {
        when(em.find(User.class, 1L)).thenReturn(testUser);
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        when(inventoryCache.getAvailableStock(100L)).thenReturn(10);
        doNothing().when(orderPlacedEvent).fire(any());

        OrderDTO result = orderService.placeOrder(validRequest);

        assertNotNull(result);
        assertTrue(result.getOrderNumber().startsWith("TM-"));
        assertEquals(1L, result.getUserId());
        verify(em).persist(any(Order.class));
        verify(em).flush();
        verify(orderQueueProducer).sendOrderForProcessing(anyString(), anyString());
        verify(orderPlacedEvent).fire(any(OrderPlacedEvent.class));
    }

    @Test
    void placeOrder_shouldThrow_whenUserNotFound() {
        when(em.find(User.class, 1L)).thenReturn(null);

        assertThrows(OrderProcessingException.class, () -> orderService.placeOrder(validRequest));
        verify(em, never()).persist(any());
    }

    @Test
    void placeOrder_shouldThrow_whenProductNotFound() {
        when(em.find(User.class, 1L)).thenReturn(testUser);
        when(em.find(Product.class, 100L)).thenReturn(null);

        assertThrows(ProductNotFoundException.class, () -> orderService.placeOrder(validRequest));
    }

    @Test
    void placeOrder_shouldThrow_whenInsufficientStock() {
        when(em.find(User.class, 1L)).thenReturn(testUser);
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        when(inventoryCache.getAvailableStock(100L)).thenReturn(1);

        assertThrows(InsufficientInventoryException.class, () -> orderService.placeOrder(validRequest));
    }

    @Test
    void findById_shouldReturnOrder_whenExists() {
        when(em.find(Order.class, 1L)).thenReturn(testOrder);

        Optional<OrderDTO> result = orderService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("TM-123-ABC", result.get().getOrderNumber());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        when(em.find(Order.class, 999L)).thenReturn(null);

        Optional<OrderDTO> result = orderService.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void findByOrderNumber_shouldReturnOrder_whenFound() {
        when(em.createNamedQuery("Order.findByOrderNumber", Order.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("orderNumber", "TM-123-ABC")).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(testOrder);

        Optional<OrderDTO> result = orderService.findByOrderNumber("TM-123-ABC");

        assertTrue(result.isPresent());
        assertEquals("TM-123-ABC", result.get().getOrderNumber());
    }

    @Test
    void findByOrderNumber_shouldReturnEmpty_whenNotFound() {
        when(em.createNamedQuery("Order.findByOrderNumber", Order.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("orderNumber", "NONEXISTENT")).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(new NoResultException());

        Optional<OrderDTO> result = orderService.findByOrderNumber("NONEXISTENT");

        assertFalse(result.isPresent());
    }

    @Test
    void cancelOrder_shouldSucceed_whenOrderIsPending() {
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(em.find(Order.class, 1L)).thenReturn(testOrder);

        OrderDTO result = orderService.cancelOrder(1L, "Changed my mind");

        assertEquals("CANCELLED", result.getStatus());
        verify(em).merge(testOrder);
    }

    @Test
    void cancelOrder_shouldThrow_whenOrderIsShipped() {
        testOrder.setStatus(Order.OrderStatus.SHIPPED);
        when(em.find(Order.class, 1L)).thenReturn(testOrder);

        assertThrows(OrderProcessingException.class, () -> orderService.cancelOrder(1L, "Too late"));
    }

    @Test
    void cancelOrder_shouldThrow_whenOrderNotFound() {
        when(em.find(Order.class, 999L)).thenReturn(null);

        assertThrows(OrderProcessingException.class, () -> orderService.cancelOrder(999L, "reason"));
    }

    @Test
    void updateOrderStatus_shouldSetTimestamps_whenShipped() {
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(em.find(Order.class, 1L)).thenReturn(testOrder);
        when(em.merge(testOrder)).thenReturn(testOrder);

        OrderDTO result = orderService.updateOrderStatus(1L, Order.OrderStatus.SHIPPED);

        assertEquals("SHIPPED", result.getStatus());
        assertNotNull(testOrder.getShippedAt());
    }

    @Test
    void updateOrderStatus_shouldSetTimestamps_whenDelivered() {
        testOrder.setStatus(Order.OrderStatus.SHIPPED);
        when(em.find(Order.class, 1L)).thenReturn(testOrder);
        when(em.merge(testOrder)).thenReturn(testOrder);

        OrderDTO result = orderService.updateOrderStatus(1L, Order.OrderStatus.DELIVERED);

        assertEquals("DELIVERED", result.getStatus());
        assertNotNull(testOrder.getDeliveredAt());
    }

    @Test
    void updateOrderStatus_shouldThrow_whenOrderNotFound() {
        when(em.find(Order.class, 999L)).thenReturn(null);

        assertThrows(OrderProcessingException.class, () -> orderService.updateOrderStatus(999L, Order.OrderStatus.CONFIRMED));
    }

    @Test
    void generateOrderNumber_shouldStartWithTM() {
        String number = orderService.generateOrderNumber();
        assertTrue(number.startsWith("TM-"));
        assertTrue(number.length() >= 18);
        assertTrue(number.contains("-"));
    }

    @Test
    void countOrdersByStatus_shouldReturnCount() {
        when(em.createNamedQuery("Order.countByStatus", Long.class)).thenReturn(mock(TypedQuery.class));
        TypedQuery<Long> countQuery = em.createNamedQuery("Order.countByStatus", Long.class);
        when(countQuery.setParameter("status", "PENDING")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(5L);

        long count = orderService.countOrdersByStatus(Order.OrderStatus.PENDING);

        assertEquals(5L, count);
    }

    @Test
    void calculateTodayRevenue_shouldReturnZero_whenNoRevenue() {
        Query revenueQuery = mock(Query.class);
        when(em.createNamedQuery("Order.sumRevenueToday")).thenReturn(revenueQuery);
        when(revenueQuery.setParameter(eq("startOfDay"), any())).thenReturn(revenueQuery);
        when(revenueQuery.getSingleResult()).thenReturn(null);

        BigDecimal revenue = orderService.calculateTodayRevenue();

        assertEquals(BigDecimal.ZERO, revenue);
    }
}
