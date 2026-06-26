package com.techmart.rest;

import com.techmart.dto.ApiResponseDTO;
import com.techmart.ejb.InventoryCacheBean;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * JAX-RS REST Resource — Inventory Management API.
 *
 * Endpoints:
 *   GET  /api/inventory/stock/{productId}              → Get total available stock
 *   GET  /api/inventory/stock/{productId}/{warehouseId}→ Get stock at warehouse
 *   GET  /api/inventory/check/{productId}              → Check availability
 *   POST /api/inventory/cache/refresh                  → Force cache refresh (admin)
 *   GET  /api/inventory/cache/snapshot                 → Full cache snapshot
 */
@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @EJB
    private InventoryCacheBean inventoryCache;

    @GET
    @Path("/stock/{productId}")
    public Response getStock(@PathParam("productId") Long productId) {
        int stock = inventoryCache.getAvailableStock(productId);
        return Response.ok(
            ApiResponseDTO.success(Map.of(
                "productId", productId,
                "availableStock", stock,
                "inStock", stock > 0
            ), "Stock level retrieved")).build();
    }

    @GET
    @Path("/stock/{productId}/{warehouseId}")
    public Response getStockAtWarehouse(
            @PathParam("productId")  Long productId,
            @PathParam("warehouseId") Long warehouseId) {
        int stock = inventoryCache.getStockAtWarehouse(productId, warehouseId);
        return Response.ok(
            ApiResponseDTO.success(Map.of(
                "productId",   productId,
                "warehouseId", warehouseId,
                "stock",       stock
            ), "Warehouse stock retrieved")).build();
    }

    @GET
    @Path("/check/{productId}")
    public Response checkAvailability(
            @PathParam("productId") Long productId,
            @QueryParam("quantity") @DefaultValue("1") int quantity) {
        boolean available = inventoryCache.hasEnoughStock(productId, quantity);
        int stock = inventoryCache.getAvailableStock(productId);
        return Response.ok(
            ApiResponseDTO.success(Map.of(
                "productId",         productId,
                "requestedQuantity", quantity,
                "availableStock",    stock,
                "available",         available
            ), available ? "Stock available" : "Insufficient stock"))
            .build();
    }

    @POST
    @Path("/cache/refresh")
    public Response refreshCache() {
        inventoryCache.refreshAll();
        return Response.ok(
            ApiResponseDTO.success(Map.of(
                "cacheSize",   inventoryCache.getCacheSize(),
                "refreshedAt", System.currentTimeMillis()
            ), "Inventory cache refreshed")).build();
    }

    @POST
    @Path("/cache/refresh/{productId}")
    public Response refreshProduct(@PathParam("productId") Long productId) {
        int newStock = inventoryCache.refreshProduct(productId);
        return Response.ok(
            ApiResponseDTO.success(Map.of(
                "productId", productId,
                "newStock",  newStock
            ), "Product cache refreshed")).build();
    }

    @GET
    @Path("/cache/snapshot")
    public Response getCacheSnapshot() {
        Map<Long, Integer> snapshot = inventoryCache.getCacheSnapshot();
        return Response.ok(
            ApiResponseDTO.success(snapshot,
                "Cache snapshot — " + snapshot.size() + " products",
                snapshot.size())).build();
    }

    @GET
    @Path("/cache/stats")
    public Response getCacheStats() {
        return Response.ok(ApiResponseDTO.success(Map.of(
            "cacheSize",       inventoryCache.getCacheSize(),
            "cacheHits",       inventoryCache.getCacheHits(),
            "cacheMisses",     inventoryCache.getCacheMisses(),
            "hitRatePct",      Math.round(inventoryCache.getHitRate() * 100.0) / 100.0,
            "lastRefreshedAt", inventoryCache.getLastRefreshTime()
        ), "Cache statistics")).build();
    }
}
