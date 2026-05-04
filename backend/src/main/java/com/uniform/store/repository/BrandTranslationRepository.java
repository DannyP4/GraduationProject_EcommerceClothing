package com.uniform.store.repository;

import com.uniform.store.entity.BrandTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BrandTranslationRepository extends JpaRepository<BrandTranslation, Long> {

    List<BrandTranslation> findByBrandIdInAndLocale(Collection<Long> brandIds, String locale);
}
