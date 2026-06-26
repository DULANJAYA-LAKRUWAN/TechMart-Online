package com.techmart.ejb;

import com.techmart.dto.CartItemDTO;
import com.techmart.dto.OrderRequestDTO;
import com.techmart.entity.Product;
import com.techmart.exception.ProductNotFoundException;
import com.techmart.exception.TechMartException;

import javax.ejb.*;
import javax.persistence.PersistenceContextType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * Stateful Session Bean — Shopping Cart.
 *
 * Design Decisions:
 * - @Stateful: Each client session has its own bean instance — cart is server-side.
 *   This is the canonical use case for Stateful EJBs: conversational state.
 * - @StatefulTimeout: Auto-remove after 30 minutes of inactivity — prevents memory leaks.
 * - Cart items stored in-memory HashMap for O(1) lookup by product ID.
 * - @Remove on checkout/clear — signals the container to destroy the bean after the call.
 * - Serializable: required for passivation (container can swap to disk if memory is low).
 *
 * Note: For massive scale (10,000 users) Stateful EJBs would be replaced with Redis-backed
 * sessions. Demonstrated here to fulfill the BCDI assessment requirement.
 */
@Stateful
@StatefulTimeout(value = 30, unit = java.util.concurrent.TimeUnit.MINUTES)
public class ShoppingCartBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ShoppingCartBean.class.getName());
    private static final int MAX_CART_ITEMS  = 50;
    private static final int MAX_ITEM_QTY    = 99;

    @PersistenceContext(unitName = "TechMartPU",
        type = PersistenceContextType.EXTENDED)   // Extended PC survives across calls
    private EntityManager em;

    /** Cart contents: productId → CartItemDTO */
    private final Map<Long, CartItemDTO> cartItems = new LinkedHashMap<>();

    private Long   userId;
    private String sessionId;

    // ── Session Initialization ────────────────────────────────────────────────

    public void initSession(Long userId, String sessionId) {
        this.userId    = userId;
        this.sessionId = sessionId;
        LOG.info("Shopping cart initialized for user: " + userId);
    }

    // ── Add to Cart ──────────────────────────────────────────────────────────

    public CartItemDTO addItem(Long productId, int quantity) {
        validateQuantity(quantity);

        if (cartItems.size() >= MAX_CART_ITEMS && !cartItems.containsKey(productId)) {
            throw new TechMartException("Cart is full (max " + MAX_CART_ITEMS + " items)", "CART_FULL");
        }

        Product product = em.find(Product.class, productId);
        if (product == null || !product.isActive()) {
            throw new ProductNotFoundException(productId);
        }

        if (cartItems.containsKey(productId)) {
            // Update existing item
            CartItemDTO existing = cartItems.get(productId);
            int newQty = Math.min(existing.getQuantity() + quantity, MAX_ITEM_QTY);
            existing.setQuantityAndRecalculate(newQty);
            LOG.fine("Updated cart item: " + productId + " qty=" + newQty);
            return existing;
        } else {
            CartItemDTO item = new CartItemDTO(
                productId, product.getName(), product.getSku(),
                quantity, product.getPrice());
            item.setImageUrl(product.getImageUrl());
            cartItems.put(productId, item);
            LOG.fine("Added to cart: " + productId);
            return item;
        }
    }

    // ── Update Quantity ──────────────────────────────────────────────────────

    public CartItemDTO updateQuantity(Long productId, int quantity) {
        validateQuantity(quantity);
        CartItemDTO item = cartItems.get(productId);
        if (item == null) {
            throw new TechMartException("Product not in cart: " + productId, "ITEM_NOT_IN_CART");
        }
        item.setQuantityAndRecalculate(quantity);
        return item;
    }

    // ── Remove Item ──────────────────────────────────────────────────────────

    public void removeItem(Long productId) {
        if (cartItems.remove(productId) == null) {
            throw new TechMartException("Product not in cart: " + productId, "ITEM_NOT_IN_CART");
        }
    }

    // ── View Cart ────────────────────────────────────────────────────────────

    public List<CartItemDTO> getItems() {
        return new ArrayList<>(cartItems.values());
    }

    public int getItemCount() {
        return cartItems.values().stream().mapToInt(CartItemDTO::getQuantity).sum();
    }

    public boolean isEmpty() {
        return cartItems.isEmpty();
    }

    public boolean containsProduct(Long productId) {
        return cartItems.containsKey(productId);
    }

    // ── Totals ───────────────────────────────────────────────────────────────

    public BigDecimal getSubtotal() {
        return cartItems.values().stream()
            .map(CartItemDTO::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getShippingCost() {
        BigDecimal subtotal = getSubtotal();
        // Free shipping over $100
        return subtotal.compareTo(new BigDecimal("100.00")) >= 0
            ? BigDecimal.ZERO
            : new BigDecimal("5.99");
    }

    public BigDecimal getTotal() {
        return getSubtotal().add(getShippingCost());
    }

    // ── Checkout — builds the order request and @Remove's the bean ───────────

    @Remove
    public OrderRequestDTO checkout(String shippingAddress, String paymentMethod) {
        if (isEmpty()) {
            throw new TechMartException("Cannot checkout with empty cart", "EMPTY_CART");
        }

        OrderRequestDTO request = new OrderRequestDTO();
        request.setUserId(userId);
        request.setShippingAddress(shippingAddress);
        request.setPaymentMethod(paymentMethod);

        List<OrderRequestDTO.OrderItemRequest> items = new ArrayList<>();
        for (CartItemDTO cartItem : cartItems.values()) {
            OrderRequestDTO.OrderItemRequest itemReq = new OrderRequestDTO.OrderItemRequest(
                cartItem.getProductId(), cartItem.getQuantity());
            items.add(itemReq);
        }
        request.setItems(items);

        LOG.info("Checkout initiated for user: " + userId + " | Items: " + items.size()
            + " | Total: " + getTotal());
        return request;
    }

    // ── Clear cart (without checkout) ────────────────────────────────────────

    @Remove
    public void clear() {
        cartItems.clear();
        LOG.info("Cart cleared for user: " + userId);
    }

    // ── Private Validation ───────────────────────────────────────────────────

    private void validateQuantity(int quantity) {
        if (quantity < 1) throw new TechMartException("Quantity must be at least 1", "INVALID_QUANTITY");
        if (quantity > MAX_ITEM_QTY) throw new TechMartException(
            "Quantity exceeds maximum (" + MAX_ITEM_QTY + ")", "QUANTITY_EXCEEDED");
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getUserId()    { return userId;    }
    public String getSessionId() { return sessionId; }
}
