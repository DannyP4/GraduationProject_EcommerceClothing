package com.uniform.store.repository;

import com.uniform.store.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByIsPrimaryDescSortOrderAsc(Long productId);

    long countByProductId(Long productId);

    @Query("""
        SELECT i FROM ProductImage i
        WHERE i.product.id IN :productIds
        ORDER BY i.product.id ASC, i.isPrimary DESC, i.sortOrder ASC
        """)
    List<ProductImage> findThumbnailCandidatesByProductIds(@Param("productIds") Collection<Long> productIds);
}
