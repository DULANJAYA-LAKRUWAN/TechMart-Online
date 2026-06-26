package com.techmart.rest;

import com.techmart.dto.ApiResponseDTO;
import com.techmart.dto.ProductDTO;
import com.techmart.service.ProductService;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JAX-RS REST Resource — Product Catalog API.
 *
 * Endpoints:
 *   GET    /api/products                   → Paginated product list
 *   GET    /api/products/{id}              → Product by ID
 *   GET    /api/products/sku/{sku}         → Product by SKU
 *   GET    /api/products/search?q=         → Full-text search
 *   GET    /api/products/category/{catId}  → Products by category
 *   GET    /api/products/featured          → Featured products
 *   GET    /api/products/price-range       → Price range filter
 *   POST   /api/products                   → Create product (admin)
 *   PUT    /api/products/{id}              → Update product (admin)
 *   DELETE /api/products/{id}              → Soft-delete product (admin)
 *   GET    /api/products/count             → Total product count
 */
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @EJB
    private ProductService productService;

    @GET
    public Response getAllProducts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        List<ProductDTO> products = productService.findAll(page, size);
        long total = productService.countProducts();
        return Response.ok(
            ApiResponseDTO.success(products, "Products retrieved", (int) total)).build();
    }

    @GET
    @Path("/{id}")
    public Response getProductById(@PathParam("id") Long id) {
        Optional<ProductDTO> product = productService.findById(id);
        return product.map(p -> Response.ok(ApiResponseDTO.success(p)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponseDTO.error("Product not found: " + id, "PRODUCT_NOT_FOUND"))
                .build());
    }

    @GET
    @Path("/sku/{sku}")
    public Response getProductBySku(@PathParam("sku") String sku) {
        Optional<ProductDTO> product = productService.findBySku(sku);
        return product.map(p -> Response.ok(ApiResponseDTO.success(p)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponseDTO.error("Product not found with SKU: " + sku, "PRODUCT_NOT_FOUND"))
                .build());
    }

    @GET
    @Path("/search")
    public Response searchProducts(
            @QueryParam("q") String keyword,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponseDTO.error("Search keyword is required", "MISSING_KEYWORD"))
                .build();
        }
        List<ProductDTO> results = productService.search(keyword.trim(), page, size);
        return Response.ok(
            ApiResponseDTO.success(results, "Search results for: " + keyword, results.size()))
            .build();
    }

    @GET
    @Path("/category/{categoryId}")
    public Response getProductsByCategory(
            @PathParam("categoryId") Long categoryId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        List<ProductDTO> products = productService.findByCategory(categoryId, page, size);
        return Response.ok(ApiResponseDTO.success(products, "Products in category", products.size())).build();
    }

    @GET
    @Path("/featured")
    public Response getFeaturedProducts(@QueryParam("limit") @DefaultValue("8") int limit) {
        List<ProductDTO> featured = productService.findFeatured(limit);
        return Response.ok(ApiResponseDTO.success(featured, "Featured products")).build();
    }

    @GET
    @Path("/price-range")
    public Response getByPriceRange(
            @QueryParam("min") @DefaultValue("0") BigDecimal min,
            @QueryParam("max") @DefaultValue("99999") BigDecimal max) {
        List<ProductDTO> products = productService.findByPriceRange(min, max);
        return Response.ok(
            ApiResponseDTO.success(products, "Products in price range $" + min + " - $" + max,
                products.size())).build();
    }

    @POST
    public Response createProduct(Map<String, String> body) {
        BigDecimal price = new BigDecimal(body.getOrDefault("price", "0"));
        Long categoryId  = body.containsKey("categoryId")
            ? Long.parseLong(body.get("categoryId")) : null;

        ProductDTO created = productService.createProduct(
            body.get("name"), body.get("sku"), body.get("description"),
            price, categoryId, body.get("brand"));

        return Response.status(Response.Status.CREATED)
            .entity(ApiResponseDTO.success(created, "Product created")).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateProduct(@PathParam("id") Long id, Map<String, String> body) {
        BigDecimal price        = new BigDecimal(body.getOrDefault("price", "0"));
        BigDecimal comparePrice = body.containsKey("comparePrice")
            ? new BigDecimal(body.get("comparePrice")) : null;
        boolean featured        = Boolean.parseBoolean(body.getOrDefault("featured", "false"));

        ProductDTO updated = productService.updateProduct(
            id, body.get("name"), body.get("description"), price, comparePrice, featured);
        return Response.ok(ApiResponseDTO.success(updated, "Product updated")).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteProduct(@PathParam("id") Long id) {
        productService.deleteProduct(id);
        return Response.ok(ApiResponseDTO.success(null, "Product deactivated")).build();
    }

    @GET
    @Path("/count")
    public Response countProducts() {
        return Response.ok(
            ApiResponseDTO.success(productService.countProducts(), "Total product count")).build();
    }
}
