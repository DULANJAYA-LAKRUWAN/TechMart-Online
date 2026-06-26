package com.techmart.unit.ejb;

import com.techmart.dto.CartItemDTO;
import com.techmart.dto.OrderRequestDTO;
import com.techmart.ejb.ShoppingCartBean;
import com.techmart.entity.Category;
import com.techmart.entity.Product;
import com.techmart.exception.ProductNotFoundException;
import com.techmart.exception.TechMartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartBeanTest {

    @Mock private EntityManager em;

    @InjectMocks
    private ShoppingCartBean cart;

    private Product testProduct;
    private Product secondProduct;

    @BeforeEach
    void setUp() {
        Category cat = new Category("Electronics", "Electronic items");
        cat.setId(10L);

        testProduct = new Product("Laptop", "LAP-001", new BigDecimal("999.99"), cat);
        testProduct.setId(100L);

        secondProduct = new Product("Mouse", "MOU-001", new BigDecimal("29.99"), cat);
        secondProduct.setId(101L);

        cart.initSession(1L, "session-abc-123");
    }

    @Test
    void addItem_shouldAddNewItem_whenNotInCart() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);

        CartItemDTO item = cart.addItem(100L, 2);

        assertNotNull(item);
        assertEquals(100L, item.getProductId());
        assertEquals(2, item.getQuantity());
        assertEquals(0, new BigDecimal("1999.98").compareTo(item.getSubtotal()));
        assertEquals(2, cart.getItemCount());
    }

    @Test
    void addItem_shouldIncrementQuantity_whenAlreadyInCart() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);

        cart.addItem(100L, 2);
        CartItemDTO item = cart.addItem(100L, 3);

        assertEquals(5, item.getQuantity());
        assertEquals(1, cart.getItems().size());
    }

    @Test
    void addItem_shouldThrow_whenProductNotFound() {
        when(em.find(Product.class, 999L)).thenReturn(null);

        assertThrows(ProductNotFoundException.class, () -> cart.addItem(999L, 1));
        assertTrue(cart.isEmpty());
    }

    @Test
    void addItem_shouldThrow_whenProductInactive() {
        testProduct.setActive(false);
        when(em.find(Product.class, 100L)).thenReturn(testProduct);

        assertThrows(ProductNotFoundException.class, () -> cart.addItem(100L, 1));
    }

    @Test
    void addItem_shouldThrow_whenQuantityExceedsMax() {
        assertThrows(TechMartException.class, () -> cart.addItem(100L, 100));
    }

    @Test
    void addItem_shouldThrow_whenQuantityBelowMin() {
        assertThrows(TechMartException.class, () -> cart.addItem(100L, 0));
    }

    @Test
    void addItem_shouldThrow_whenCartIsFull() {
        when(em.find(eq(Product.class), anyLong())).thenReturn(testProduct);

        // Fill cart to max items (50) — each with a unique productId
        for (long i = 1; i <= 50; i++) {
            Product p = new Product("Product " + i, "SKU-" + i, BigDecimal.TEN, null);
            p.setId(100L + i);
            when(em.find(Product.class, 100L + i)).thenReturn(p);
            cart.addItem(100L + i, 1);
        }

        assertThrows(TechMartException.class, () -> cart.addItem(999L, 1));
    }

    @Test
    void updateQuantity_shouldModifyExistingItem() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        cart.addItem(100L, 2);

        CartItemDTO updated = cart.updateQuantity(100L, 5);

        assertEquals(5, updated.getQuantity());
        assertEquals(0, new BigDecimal("4999.95").compareTo(updated.getSubtotal()));
    }

    @Test
    void updateQuantity_shouldThrow_whenItemNotInCart() {
        assertThrows(TechMartException.class, () -> cart.updateQuantity(100L, 3));
    }

    @Test
    void removeItem_shouldRemove_whenExists() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        cart.addItem(100L, 2);

        cart.removeItem(100L);

        assertTrue(cart.isEmpty());
    }

    @Test
    void removeItem_shouldThrow_whenNotInCart() {
        assertThrows(TechMartException.class, () -> cart.removeItem(999L));
    }

    @Test
    void getSubtotal_shouldSumAllItems() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        when(em.find(Product.class, 101L)).thenReturn(secondProduct);

        cart.addItem(100L, 2);   // 999.99 * 2 = 1999.98
        cart.addItem(101L, 3);   // 29.99 * 3 = 89.97

        assertEquals(0, new BigDecimal("2089.95").compareTo(cart.getSubtotal()));
    }

    @Test
    void shippingCost_shouldBeFree_whenSubtotalOver100() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        cart.addItem(100L, 1);   // 999.99

        assertEquals(BigDecimal.ZERO, cart.getShippingCost());
    }

    @Test
    void shippingCost_shouldBe599_whenSubtotalUnder100() {
        when(em.find(Product.class, 101L)).thenReturn(secondProduct);
        cart.addItem(101L, 1);   // 29.99

        assertEquals(0, new BigDecimal("5.99").compareTo(cart.getShippingCost()));
    }

    @Test
    void getTotal_shouldIncludeShipping() {
        when(em.find(Product.class, 101L)).thenReturn(secondProduct);
        cart.addItem(101L, 1);   // 29.99 + 5.99 = 35.98

        assertEquals(0, new BigDecimal("35.98").compareTo(cart.getTotal()));
    }

    @Test
    void checkout_shouldBuildOrderRequest() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        when(em.find(Product.class, 101L)).thenReturn(secondProduct);

        cart.addItem(100L, 1);
        cart.addItem(101L, 2);

        OrderRequestDTO request = cart.checkout("123 Main St", "CREDIT_CARD");

        assertNotNull(request);
        assertEquals(1L, request.getUserId());
        assertEquals("123 Main St", request.getShippingAddress());
        assertEquals("CREDIT_CARD", request.getPaymentMethod());
        assertEquals(2, request.getItems().size());
    }

    @Test
    void checkout_shouldThrow_whenCartIsEmpty() {
        assertThrows(TechMartException.class,
            () -> cart.checkout("addr", "CARD"));
    }

    @Test
    void containsProduct_shouldReturnCorrectly() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        cart.addItem(100L, 1);

        assertTrue(cart.containsProduct(100L));
        assertFalse(cart.containsProduct(999L));
    }

    @Test
    void getItemCount_shouldReturnSumOfQuantities() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        when(em.find(Product.class, 101L)).thenReturn(secondProduct);

        cart.addItem(100L, 2);
        cart.addItem(101L, 3);

        assertEquals(5, cart.getItemCount());
    }

    @Test
    void getItems_shouldReturnAllItems() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        cart.addItem(100L, 2);

        List<CartItemDTO> items = cart.getItems();

        assertEquals(1, items.size());
        assertEquals(100L, items.get(0).getProductId());
    }

    @Test
    void clear_shouldEmptyCart() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        cart.addItem(100L, 2);
        assertFalse(cart.isEmpty());

        cart.clear();

        assertTrue(cart.isEmpty());
    }
}
