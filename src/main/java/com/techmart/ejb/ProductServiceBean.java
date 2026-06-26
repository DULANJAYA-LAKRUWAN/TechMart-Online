package com.techmart.ejb;

import com.techmart.cdi.Auditable;
import com.techmart.cdi.Monitored;
import com.techmart.dto.ProductDTO;
import com.techmart.entity.Category;
import com.techmart.entity.Product;
import com.techmart.exception.ProductNotFoundException;
import com.techmart.service.ProductService;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Stateless Session Bean — Product Catalog Management.
 *
 * Design Decisions:
 * - @Stateless: Pooled by the container — no per-client state needed for catalog ops.
 * - Pagination on all list queries prevents OOM from large catalogs.
 * - JPQL NamedQueries are pre-compiled at deployment time for performance.
 * - READ operations use SUPPORTS transaction to allow read replicas in cluster.
 * - @Auditable only on write operations to minimize audit log noise.
 */
@Stateless
@Monitored
public class ProductServiceBean implements ProductService {

    private static final Logger LOG = Logger.getLogger(ProductServiceBean.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    // ── Create ───────────────────────────────────────────────────────────────

    @Override
    @Auditable(action = "CREATE_PRODUCT", entity = "Product")
    public ProductDTO createProduct(String name, String sku, String description,
                                    BigDecimal price, Long categoryId, String brand) {
        Category category = null;
        if (categoryId != null) {
            category = em.find(Category.class, categoryId);
            if (category == null) {
                throw new RuntimeException("Category not found: " + categoryId);
            }
        }

        Product product = new Product(name, sku, price, category);
        product.setDescription(description);
        product.setBrand(brand);

        em.persist(product);
        em.flush();
        LOG.info("Created product: " + sku);
        return toDTO(product);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Override
    @Auditable(action = "UPDATE_PRODUCT", entity = "Product")
    public ProductDTO updateProduct(Long id, String name, String description,
                                    BigDecimal price, BigDecimal comparePrice, boolean featured) {
        Product product = findEntityOrThrow(id);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setComparePrice(comparePrice);
        product.setFeatured(featured);
        return toDTO(em.merge(product));
    }

    // ── Find Operations ──────────────────────────────────────────────────────

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<ProductDTO> findById(Long id) {
        Product product = em.find(Product.class, id);
        return (product != null && product.isActive())
            ? Optional.of(toDTO(product))
            : Optional.empty();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<ProductDTO> findBySku(String sku) {
        try {
            Product product = em.createNamedQuery("Product.findBySku", Product.class)
                .setParameter("sku", sku)
                .getSingleResult();
            return Optional.of(toDTO(product));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ProductDTO> findAll(int page, int size) {
        return em.createNamedQuery("Product.findAll", Product.class)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ProductDTO> findByCategory(Long categoryId, int page, int size) {
        return em.createNamedQuery("Product.findByCategory", Product.class)
            .setParameter("categoryId", categoryId)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ProductDTO> search(String keyword, int page, int size) {
        return em.createNamedQuery("Product.searchByName", Product.class)
            .setParameter("name", keyword)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ProductDTO> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return em.createNamedQuery("Product.findByPriceRange", Product.class)
            .setParameter("minPrice", minPrice)
            .setParameter("maxPrice", maxPrice)
            .getResultList()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ProductDTO> findFeatured(int limit) {
        return em.createNamedQuery("Product.findFeatured", Product.class)
            .setMaxResults(limit)
            .getResultList()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Override
    @Auditable(action = "DELETE_PRODUCT", entity = "Product")
    public void deleteProduct(Long id) {
        Product product = findEntityOrThrow(id);
        product.setActive(false);
        em.merge(product);
        LOG.info("Soft-deleted product: " + product.getSku());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long countProducts() {
        return em.createNamedQuery("Product.countAll", Long.class).getSingleResult();
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private Product findEntityOrThrow(Long id) {
        Product product = em.find(Product.class, id);
        if (product == null || !product.isActive()) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    private ProductDTO toDTO(Product p) {
        ProductDTO dto = new ProductDTO(p.getId(), p.getName(), p.getSku(), p.getPrice());
        dto.setDescription(p.getDescription());
        dto.setComparePrice(p.getComparePrice());
        dto.setImageUrl(p.getImageUrl());
        dto.setBrand(p.getBrand());
        dto.setFeatured(p.isFeatured());
        dto.setActive(p.isActive());
        dto.setOnSale(p.isOnSale());
        dto.setDiscountPercentage(p.getDiscountPercentage());
        if (p.getCategory() != null) {
            dto.setCategoryId(p.getCategory().getId());
            dto.setCategoryName(p.getCategory().getName());
        }
        return dto;
    }
}
