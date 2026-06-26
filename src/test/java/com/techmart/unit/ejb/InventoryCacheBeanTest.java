package com.techmart.unit.ejb;

import com.techmart.ejb.InventoryCacheBean;
import com.techmart.entity.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryCacheBeanTest {

    @Mock private EntityManager em;
    @Mock private Query inventoryQuery;
    @Mock private Query warehouseQuery;

    @InjectMocks
    private InventoryCacheBean inventoryCache;

    private List<Object[]> dbResults;

    @BeforeEach
    void setUp() {
        dbResults = new ArrayList<>();
        dbResults.add(new Object[]{100L, 50L});    // product 100, stock 50
        dbResults.add(new Object[]{101L, 10L});    // product 101, stock 10
        dbResults.add(new Object[]{102L, 0L});     // product 102, stock 0
    }

    @Test
    void getAvailableStock_shouldReturnCachedValue_onHit() {
        when(em.createQuery(anyString())).thenReturn(inventoryQuery);
        when(inventoryQuery.getResultList()).thenReturn(dbResults);
        inventoryCache.initialize();
        verify(em).createQuery(contains("Inventory i GROUP BY"));

        int stock = inventoryCache.getAvailableStock(100L);

        assertEquals(50, stock);
        verify(em, never()).createNamedQuery("Inventory.findTotalStockByProduct");
    }

    @Test
    void getAvailableStock_shouldLoadFromDb_onMiss() {
        when(em.createQuery(anyString())).thenReturn(inventoryQuery);
        when(inventoryQuery.getResultList()).thenReturn(dbResults);
        inventoryCache.initialize();

        when(em.createNamedQuery("Inventory.findTotalStockByProduct")).thenReturn(warehouseQuery);
        when(warehouseQuery.setParameter("productId", 999L)).thenReturn(warehouseQuery);
        when(warehouseQuery.getSingleResult()).thenReturn(25L);

        int stock = inventoryCache.getAvailableStock(999L);

        assertEquals(25, stock);
    }

    @Test
    void getAvailableStock_shouldReturnZero_onDbException() {
        when(em.createQuery(anyString())).thenReturn(inventoryQuery);
        when(inventoryQuery.getResultList()).thenReturn(dbResults);
        inventoryCache.initialize();

        when(em.createNamedQuery("Inventory.findTotalStockByProduct")).thenReturn(warehouseQuery);
        when(warehouseQuery.setParameter("productId", 999L)).thenReturn(warehouseQuery);
        when(warehouseQuery.getSingleResult()).thenThrow(new RuntimeException("DB error"));

        int stock = inventoryCache.getAvailableStock(999L);

        assertEquals(0, stock);
    }

    @Test
    void hasEnoughStock_shouldReturnTrue_whenStockSufficient() {
        when(em.createQuery(anyString())).thenReturn(inventoryQuery);
        when(inventoryQuery.getResultList()).thenReturn(dbResults);
        inventoryCache.initialize();

        assertTrue(inventoryCache.hasEnoughStock(100L, 10));
        assertTrue(inventoryCache.hasEnoughStock(100L, 50));
    }

    @Test
    void hasEnoughStock_shouldReturnFalse_whenStockInsufficient() {
        when(em.createQuery(anyString())).thenReturn(inventoryQuery);
        when(inventoryQuery.getResultList()).thenReturn(dbResults);
        inventoryCache.initialize();

        assertFalse(inventoryCache.hasEnoughStock(100L, 51));
        assertFalse(inventoryCache.hasEnoughStock(102L, 1));
    }

    @Test
    void evict_shouldRemoveFromCache() {
        when(em.createQuery(anyString())).thenReturn(inventoryQuery);
        when(inventoryQuery.getResultList()).thenReturn(dbResults);
        inventoryCache.initialize();
        assertEquals(3, inventoryCache.getCacheSize());

        inventoryCache.evict(100L);

        assertEquals(2, inventoryCache.getCacheSize());
        assertFalse(inventoryCache.getCacheSnapshot().containsKey(100L));
    }

    @Test
    void refreshAll_shouldReloadFromDatabase() {
        when(em.createQuery(anyString())).thenReturn(inventoryQuery);
        when(inventoryQuery.getResultList()).thenReturn(dbResults);
        inventoryCache.initialize();

        List<Object[]> newResults = new ArrayList<>();
        newResults.add(new Object[]{200L, 100L});
        when(em.createQuery(anyString())).thenReturn(inventoryQuery);
        when(inventoryQuery.getResultList()).thenReturn(newResults);

        inventoryCache.refreshAll();

        Map<Long, Integer> snapshot = inventoryCache.getCacheSnapshot();
        assertEquals(1, snapshot.size());
        assertTrue(snapshot.containsKey(200L));
    }

    @Test
    void getHitRate_shouldReturnCorrectPercentage() {
        when(em.createQuery(anyString())).thenReturn(inventoryQuery);
        when(inventoryQuery.getResultList()).thenReturn(dbResults);
        inventoryCache.initialize();

        assertEquals(3, inventoryCache.getCacheSize());

        // First access is a cache hit
        inventoryCache.getAvailableStock(100L);
        double hitRate = inventoryCache.getHitRate();
        assertEquals(100.0, hitRate, 0.01);

        // Then a cache miss
        when(em.createNamedQuery("Inventory.findTotalStockByProduct")).thenReturn(warehouseQuery);
        when(warehouseQuery.setParameter("productId", 999L)).thenReturn(warehouseQuery);
        when(warehouseQuery.getSingleResult()).thenReturn(25L);

        inventoryCache.getAvailableStock(999L);
        double hitRateAfterMiss = inventoryCache.getHitRate();
        assertTrue(hitRateAfterMiss < 100.0);
        assertTrue(hitRateAfterMiss > 0);
    }

    @Test
    void cacheSize_shouldReturnZero_beforeInitialization() {
        assertEquals(0, inventoryCache.getCacheSize());
    }
}
