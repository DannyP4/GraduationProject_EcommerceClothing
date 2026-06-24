package com.uniform.store.service.impl;

import com.uniform.store.config.CacheNames;
import com.uniform.store.dto.response.CategoryDto;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.CategoryTranslation;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.CategoryTranslationRepository;
import com.uniform.store.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;

    @Override
    @Cacheable(cacheNames = CacheNames.CATEGORIES, key = "#locale")
    public List<CategoryDto> listCategories(String locale) {
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc();
        if (categories.isEmpty()) {
            return List.of();
        }

        Map<Long, CategoryTranslation> translations = categoryTranslationRepository
                .findByCategoryIdInAndLocale(
                        categories.stream().map(Category::getId).toList(),
                        locale)
                .stream()
                .collect(Collectors.toMap(t -> t.getCategory().getId(), Function.identity(), (a, b) -> a));

        return categories.stream()
                .map(c -> CategoryDto.builder()
                        .id(c.getId())
                        .parentId(c.getParent() != null ? c.getParent().getId() : null)
                        .slug(c.getSlug())
                        .name(translations.containsKey(c.getId())
                                ? translations.get(c.getId()).getName()
                                : c.getName())
                        .sortOrder(c.getSortOrder())
                        .build())
                .toList();
    }
}
