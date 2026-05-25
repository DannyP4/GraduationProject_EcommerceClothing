package com.uniform.store.repository;

import com.uniform.store.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndIsActiveTrueAndDeletedAtIsNull(Long id);

    Optional<Product> findBySlugAndIsActiveTrueAndDeletedAtIsNull(String slug);

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

    long countByBrandIdAndDeletedAtIsNull(Long brandId);

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
