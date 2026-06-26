package com.techmart.service;

import javax.ejb.Local;
import java.util.Map;

/**
 * Local EJB interface for the Inventory Cache (Singleton).
 * Used by other EJBs to quickly check stock levels without hitting the DB.
 */
@Local
public interface InventoryService {

    /**
     * Get total available stock for a product across all warehouses.
     */
    int getAvailableStock(Long productId);

    /**
     * Get available stock at a specific warehouse.
     */
    int getStockAtWarehouse(Long productId, Long warehouseId);

    /**
     * Check if a product has enough stock for the requested quantity.
     */
    boolean hasEnoughStock(Long productId, int requiredQuantity);

    /**
     * Refresh the cache for a specific product (called after stock changes).
     */
    int refreshProduct(Long productId);

    /**
     * Refresh the entire inventory cache from DB.
     * Called by @Schedule timer in InventoryCacheBean.
     */
    void refreshAll();

    /**
     * Evict a product from the cache.
     */
    void evict(Long productId);

    /**
     * Return a snapshot of the entire cache for monitoring.
     */
    Map<Long, Integer> getCacheSnapshot();

    /**
     * Return the total number of products in the cache.
     */
    int getCacheSize();
}
