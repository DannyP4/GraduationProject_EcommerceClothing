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

    // Bulk fetch with product joined
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product WHERE v.id IN :ids")
    List<ProductVariant> findAllByIdInWithProduct(@Param("ids") Collection<Long> ids);

    // SELECT ... FOR UPDATE - used at order placement / cancellation to serialize stock mutations
    // on the same variants and prevent oversell when multiple users check out concurrently.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product WHERE v.id IN :ids")
    List<ProductVariant> findAllByIdInWithProductForUpdate(@Param("ids") Collection<Long> ids);
}
