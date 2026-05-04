package com.uniform.store.repository;

import com.uniform.store.entity.ProductTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductTranslationRepository extends JpaRepository<ProductTranslation, Long> {

    Optional<ProductTranslation> findByProductIdAndLocale(Long productId, String locale);

    List<ProductTranslation> findByProductIdInAndLocale(Collection<Long> productIds, String locale);
}
