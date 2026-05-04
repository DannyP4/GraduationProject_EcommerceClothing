package com.uniform.store.repository;

import com.uniform.store.entity.CategoryTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, Long> {

    List<CategoryTranslation> findByCategoryIdInAndLocale(Collection<Long> categoryIds, String locale);
}
