package com.techmart.service;

import com.techmart.dto.ProductDTO;

import javax.ejb.Local;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Local EJB interface for Product catalog management.
 * Implemented by ProductServiceBean (Stateless Session Bean).
 */
@Local
public interface ProductService {

    /**
     * Create a new product in the catalog.
     */
    ProductDTO createProduct(String name, String sku, String description,
                             BigDecimal price, Long categoryId, String brand);

    /**
     * Update existing product details.
     */
    ProductDTO updateProduct(Long id, String name, String description,
                             BigDecimal price, BigDecimal comparePrice, boolean featured);

    /**
     * Find a product by its ID.
     */
    Optional<ProductDTO> findById(Long id);

    /**
     * Find a product by its SKU.
     */
    Optional<ProductDTO> findBySku(String sku);

    /**
     * Retrieve all active products with pagination.
     * @param page  0-based page number
     * @param size  Number of records per page
     */
    List<ProductDTO> findAll(int page, int size);

    /**
     * Find all products in a specific category.
     */
    List<ProductDTO> findByCategory(Long categoryId, int page, int size);

    /**
     * Full-text search on product name and description.
     */
    List<ProductDTO> search(String keyword, int page, int size);

    /**
     * Find products within a price range.
     */
    List<ProductDTO> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Retrieve featured products for homepage display.
     */
    List<ProductDTO> findFeatured(int limit);

    /**
     * Soft-delete a product (sets active = false).
     */
    void deleteProduct(Long id);

    /**
     * Count total active products.
     */
    long countProducts();
}
