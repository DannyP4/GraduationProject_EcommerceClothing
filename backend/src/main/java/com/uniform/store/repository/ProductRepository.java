package com.uniform.store.repository;

import com.uniform.store.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndIsActiveTrueAndDeletedAtIsNull(Long id);

    Optional<Product> findBySlugAndIsActiveTrueAndDeletedAtIsNull(String slug);

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

    long countByBrandIdAndDeletedAtIsNull(Long brandId);

    boolean existsBySlug(String slug);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.deletedAt IS NULL "
            + "AND EXISTS (SELECT 1 FROM ProductImage img WHERE img.product = p AND img.publicId IS NOT NULL)")
    long countEmbeddable();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.brand LEFT JOIN FETCH p.category "
            + "WHERE p.isActive = true AND p.deletedAt IS NULL "
            + "AND EXISTS (SELECT 1 FROM ProductImage img WHERE img.product = p AND img.publicId IS NOT NULL)")
    List<Product> findAllEmbeddable();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.brand LEFT JOIN FETCH p.category "
            + "WHERE p.id IN :ids")
    List<Product> findAllByIdInWithBrandAndCategory(@Param("ids") Collection<Long> ids);

    @Query(value = """
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.brand
        LEFT JOIN FETCH p.category
        WHERE (:includeDeleted = TRUE OR p.deletedAt IS NULL)
          AND (:onlyDeleted = FALSE OR p.deletedAt IS NOT NULL)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:gender IS NULL OR p.gender = :gender)
          AND (:isActive IS NULL OR p.isActive = :isActive)
          AND (:search IS NULL
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        countQuery = """
        SELECT COUNT(p) FROM Product p
        WHERE (:includeDeleted = TRUE OR p.deletedAt IS NULL)
          AND (:onlyDeleted = FALSE OR p.deletedAt IS NOT NULL)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:gender IS NULL OR p.gender = :gender)
          AND (:isActive IS NULL OR p.isActive = :isActive)
          AND (:search IS NULL
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Product> searchAdmin(
            @Param("search") String search,
            @Param("brandId") Long brandId,
            @Param("categoryId") Long categoryId,
            @Param("gender") com.uniform.store.enums.Gender gender,
            @Param("isActive") Boolean isActive,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("onlyDeleted") boolean onlyDeleted,
            Pageable pageable);

    /**
     * Query retrieves a paginated list of Products and eagerly fetches brand
     * and category to avoid the N+1 problem.
     * @param categoryId
     * @param brandId
     * @param minPrice
     * @param maxPrice
     * @param search
     * @param pageable
     * @return Page<Product>
     */
    @Query(value = """
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.brand
        LEFT JOIN FETCH p.category
        WHERE p.isActive = true
          AND p.deletedAt IS NULL
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        countQuery = """
        SELECT COUNT(p) FROM Product p
        WHERE p.isActive = true
          AND p.deletedAt IS NULL
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Product> searchProducts(
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            Pageable pageable);
}
