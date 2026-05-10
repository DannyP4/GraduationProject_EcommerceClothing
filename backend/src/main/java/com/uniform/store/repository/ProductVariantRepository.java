package com.uniform.store.repository;

import com.uniform.store.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndIsActiveTrueOrderBySizeAscColorAsc(Long productId);

    // Bulk fetch with product joined
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product WHERE v.id IN :ids")
    List<ProductVariant> findAllByIdInWithProduct(@Param("ids") Collection<Long> ids);
}
