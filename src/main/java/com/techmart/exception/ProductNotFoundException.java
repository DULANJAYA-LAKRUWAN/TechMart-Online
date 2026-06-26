package com.techmart.exception;

/**
 * Thrown when a requested product does not exist or is not active.
 */
public class ProductNotFoundException extends TechMartException {

    private static final long serialVersionUID = 1L;

    private final Long productId;
    private final String sku;

    public ProductNotFoundException(Long productId) {
        super("Product not found with ID: " + productId, "PRODUCT_NOT_FOUND");
        this.productId = productId;
        this.sku = null;
    }

    public ProductNotFoundException(String sku) {
        super("Product not found with SKU: " + sku, "PRODUCT_NOT_FOUND");
        this.productId = null;
        this.sku = sku;
    }

    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
}
