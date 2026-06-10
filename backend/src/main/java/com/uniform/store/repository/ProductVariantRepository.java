package com.uniform.store.repository;

import com.uniform.store.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndIsActiveTrueOrderBySizeAscColorAsc(Long productId);

    List<ProductVariant> findByProductIdOrderBySizeAscColorAsc(Long productId);

    boolean existsBySku(String sku);

    boolean existsByProductIdAndSizeAndColor(Long productId, String size, String color);

    long countByIsActiveTrueAndStockQuantityLessThanEqual(int threshold);

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product"
            + " WHERE v.isActive = true AND v.stockQuantity <= :threshold"
            + " ORDER BY v.stockQuantity ASC, v.id ASC")
    List<ProductVariant> findLowStockWithProduct(@Param("threshold") int threshold);

    // Bulk fetch with product joined
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product WHERE v.id IN :ids")
    List<ProductVariant> findAllByIdInWithProduct(@Param("ids") Collection<Long> ids);

    // In-stock active variants of a category, product joined, for the co-purchase demo seeder
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product p"
            + " WHERE v.isActive = true AND v.stockQuantity > 0"
            + " AND p.isActive = true AND p.deletedAt IS NULL AND p.category.id = :categoryId"
            + " ORDER BY p.id ASC, v.id ASC")
    List<ProductVariant> findInStockByCategory(@Param("categoryId") Long categoryId);

    // SELECT ... FOR UPDATE - used at order placement / cancellation to serialize stock mutations
    // on the same variants and prevent oversell when multiple users check out concurrently.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product WHERE v.id IN :ids")
    List<ProductVariant> findAllByIdInWithProductForUpdate(@Param("ids") Collection<Long> ids);

    @Query("SELECT v.product.id AS productId, v.color AS color FROM ProductVariant v WHERE v.isActive = true")
    List<ProductColorView> findActiveProductColors();

    interface ProductColorView {
        Long getProductId();
        String getColor();
    }
}
