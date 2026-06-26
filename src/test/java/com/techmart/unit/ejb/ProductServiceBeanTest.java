package com.techmart.unit.ejb;

import com.techmart.dto.ProductDTO;
import com.techmart.ejb.ProductServiceBean;
import com.techmart.entity.Category;
import com.techmart.entity.Product;
import com.techmart.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceBeanTest {

    @Mock private EntityManager em;
    @Mock private TypedQuery<Product> typedQuery;

    @InjectMocks
    private ProductServiceBean productService;

    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testCategory = new Category("Electronics", "Electronic items");
        testCategory.setId(10L);

        testProduct = new Product("Laptop", "LAP-001", new BigDecimal("999.99"), testCategory);
        testProduct.setId(100L);
        testProduct.setBrand("TechBrand");
    }

    @Test
    void createProduct_shouldSucceed_withCategory() {
        when(em.find(Category.class, 10L)).thenReturn(testCategory);

        ProductDTO result = productService.createProduct("Laptop", "LAP-001", "A laptop",
                new BigDecimal("999.99"), 10L, "TechBrand");

        assertNotNull(result);
        assertEquals("LAP-001", result.getSku());
        assertEquals("TechBrand", result.getBrand());
        assertEquals(10L, result.getCategoryId());
        verify(em).persist(any(Product.class));
        verify(em).flush();
    }

    @Test
    void createProduct_shouldSucceed_withoutCategory() {
        ProductDTO result = productService.createProduct("Mouse", "MOU-001", "A mouse",
                new BigDecimal("29.99"), null, "TechBrand");

        assertNotNull(result);
        assertNull(result.getCategoryId());
        verify(em).persist(any(Product.class));
    }

    @Test
    void createProduct_shouldThrow_whenCategoryNotFound() {
        when(em.find(Category.class, 999L)).thenReturn(null);

        assertThrows(RuntimeException.class,
            () -> productService.createProduct("Bad", "BAD-001", "desc",
                BigDecimal.TEN, 999L, "Brand"));
    }

    @Test
    void findById_shouldReturnProduct_whenExistsAndActive() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);

        Optional<ProductDTO> result = productService.findById(100L);

        assertTrue(result.isPresent());
        assertEquals("LAP-001", result.get().getSku());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        when(em.find(Product.class, 999L)).thenReturn(null);

        Optional<ProductDTO> result = productService.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void findById_shouldReturnEmpty_whenInactive() {
        testProduct.setActive(false);
        when(em.find(Product.class, 100L)).thenReturn(testProduct);

        Optional<ProductDTO> result = productService.findById(100L);

        assertFalse(result.isPresent());
    }

    @Test
    void findBySku_shouldReturnProduct_whenFound() {
        when(em.createNamedQuery("Product.findBySku", Product.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("sku", "LAP-001")).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(testProduct);

        Optional<ProductDTO> result = productService.findBySku("LAP-001");

        assertTrue(result.isPresent());
        assertEquals("Laptop", result.get().getName());
    }

    @Test
    void findBySku_shouldReturnEmpty_whenNotFound() {
        when(em.createNamedQuery("Product.findBySku", Product.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("sku", "NONEXISTENT")).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(new NoResultException());

        Optional<ProductDTO> result = productService.findBySku("NONEXISTENT");

        assertFalse(result.isPresent());
    }

    @Test
    void updateProduct_shouldModifyAndReturn() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        when(em.merge(any(Product.class))).thenReturn(testProduct);

        ProductDTO result = productService.updateProduct(100L, "Gaming Laptop", "Updated desc",
                new BigDecimal("1299.99"), new BigDecimal("1499.99"), true);

        assertEquals("Gaming Laptop", result.getName());
        assertEquals(0, new BigDecimal("1299.99").compareTo(result.getPrice()));
        assertTrue(result.isFeatured());
    }

    @Test
    void deleteProduct_shouldSoftDelete() {
        when(em.find(Product.class, 100L)).thenReturn(testProduct);

        productService.deleteProduct(100L);

        assertFalse(testProduct.isActive());
        verify(em).merge(testProduct);
    }

    @Test
    void deleteProduct_shouldThrow_whenNotFound() {
        when(em.find(Product.class, 999L)).thenReturn(null);

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(999L));
    }

    @Test
    void search_shouldReturnResults() {
        when(em.createNamedQuery("Product.searchByName", Product.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("name", "laptop")).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(0)).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(10)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(testProduct));

        List<ProductDTO> results = productService.search("laptop", 0, 10);

        assertEquals(1, results.size());
        assertEquals("LAP-001", results.get(0).getSku());
    }

    @Test
    void findFeatured_shouldReturnFeaturedProducts() {
        when(em.createNamedQuery("Product.findFeatured", Product.class)).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(5)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(testProduct));

        List<ProductDTO> results = productService.findFeatured(5);

        assertEquals(1, results.size());
    }

    @Test
    void countProducts_shouldReturnCount() {
        TypedQuery<Long> countQuery = mock(TypedQuery.class);
        when(em.createNamedQuery("Product.countAll", Long.class)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(42L);

        long count = productService.countProducts();

        assertEquals(42L, count);
    }

    @Test
    void findByPriceRange_shouldReturnFilteredResults() {
        when(em.createNamedQuery("Product.findByPriceRange", Product.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("minPrice", new BigDecimal("10"))).thenReturn(typedQuery);
        when(typedQuery.setParameter("maxPrice", new BigDecimal("100"))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(testProduct));

        List<ProductDTO> results = productService.findByPriceRange(new BigDecimal("10"), new BigDecimal("100"));

        assertEquals(1, results.size());
    }
}
