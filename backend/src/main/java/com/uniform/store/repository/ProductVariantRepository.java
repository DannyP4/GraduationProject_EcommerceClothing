package com.uniform.store.repository;

import com.uniform.store.entity.ProductVariant;
import com.uniform.store.enums.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    Optional<ProductVariant> findByProductIdAndSizeAndColor(Long productId, ProductSize size, String color);
    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);
}
