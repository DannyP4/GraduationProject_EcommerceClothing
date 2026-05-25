package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateCategoryRequest;
import com.uniform.store.dto.request.UpdateCategoryRequest;
import com.uniform.store.dto.response.AdminCategoryDto;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.CategoryTranslation;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.CategoryTranslationRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.service.AdminCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private static final String LOCALE_EN = "en";

    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository translationRepository;
    private final ProductRepository productRepository;

    @Override
    public List<AdminCategoryDto> listTree() {
        List<Category> all = categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        if (all.isEmpty()) return List.of();

        List<Long> ids = all.stream().map(Category::getId).toList();
        Map<Long, String> enNames = new HashMap<>();
        for (CategoryTranslation t : translationRepository.findByCategoryIdIn(ids)) {
            if (LOCALE_EN.equals(t.getLocale())) enNames.put(t.getCategory().getId(), t.getName());
        }

        Map<Long, Long> productCounts = new HashMap<>();
        for (Category c : all) {
            productCounts.put(c.getId(), productRepository.countByCategoryIdAndDeletedAtIsNull(c.getId()));
        }

        Map<Long, AdminCategoryDto.AdminCategoryDtoBuilder> builders = new LinkedHashMap<>();
        for (Category c : all) {
            builders.put(c.getId(), AdminCategoryDto.builder()
                    .id(c.getId())
                    .parentId(c.getParent() != null ? c.getParent().getId() : null)
                    .slug(c.getSlug())
                    .name(c.getName())
                    .imageUrl(c.getImageUrl())
                    .nameVi(c.getName())
                    .nameEn(enNames.get(c.getId()))
                    .sortOrder(c.getSortOrder())
                    .isActive(c.getIsActive())
                    .productCount(productCounts.get(c.getId()))
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt()));
        }

        Set<Long> validIds = new HashSet<>(ids);
        Map<Long, List<AdminCategoryDto>> childrenByParent = new HashMap<>();
        Map<Long, AdminCategoryDto> dtoById = new HashMap<>();
        for (Category c : all) {
            Long rawPid = c.getParent() != null ? c.getParent().getId() : null;
            Long pid = rawPid != null && validIds.contains(rawPid) ? rawPid : null;
            AdminCategoryDto dto = builders.get(c.getId()).children(new ArrayList<>()).build();
            dtoById.put(c.getId(), dto);
            childrenByParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(dto);
        }

        Set<Long> emitted = new HashSet<>();
        List<AdminCategoryDto> roots = new ArrayList<>();
        Deque<AdminCategoryDto> stack = new ArrayDeque<>(childrenByParent.getOrDefault(null, List.of()));
        while (!stack.isEmpty()) {
            AdminCategoryDto node = stack.pop();
            if (!emitted.add(node.getId())) continue;
            roots.add(node);
            for (AdminCategoryDto kid : childrenByParent.getOrDefault(node.getId(), List.of())) {
                if (!emitted.contains(kid.getId())) {
                    node.getChildren().add(kid);
                    stack.push(kid);
                }
            }
        }
        for (Category c : all) {
            if (!emitted.contains(c.getId())) {
                AdminCategoryDto dto = dtoById.get(c.getId());
                roots.add(dto);
                emitted.add(c.getId());
            }
        }
        roots.sort(Comparator.comparing(AdminCategoryDto::getSortOrder)
                .thenComparing(AdminCategoryDto::getName, String.CASE_INSENSITIVE_ORDER));
        return roots;
    }

    @Override
    public AdminCategoryDto get(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        String enName = translationRepository.findByCategoryIdAndLocale(id, LOCALE_EN)
                .map(CategoryTranslation::getName).orElse(null);
        long products = productRepository.countByCategoryIdAndDeletedAtIsNull(id);
        return AdminCategoryDto.builder()
                .id(c.getId())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .slug(c.getSlug())
                .name(c.getName())
                .imageUrl(c.getImageUrl())
                .nameVi(c.getName())
                .nameEn(enName)
                .sortOrder(c.getSortOrder())
                .isActive(c.getIsActive())
                .productCount(products)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public AdminCategoryDto create(CreateCategoryRequest req) {
        if (categoryRepository.existsBySlug(req.getSlug())) {
            throw new BadRequestException("Category slug already exists: " + req.getSlug());
        }
        Category parent = req.getParentId() == null ? null : categoryRepository.findById(req.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getParentId()));

        Category saved = categoryRepository.save(Category.builder()
                .parent(parent)
                .slug(req.getSlug())
                .name(req.getName())
                .imageUrl(blankToNull(req.getImageUrl()))
                .sortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder())
                .isActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive())
                .build());

        if (req.getNameEn() != null && !req.getNameEn().isBlank()) {
            translationRepository.save(CategoryTranslation.builder()
                    .category(saved)
                    .locale(LOCALE_EN)
                    .name(req.getNameEn())
                    .translatedAt(Instant.now())
                    .build());
        }
        return get(saved.getId());
    }

    @Override
    @Transactional
    public AdminCategoryDto update(Long id, UpdateCategoryRequest req) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (Boolean.TRUE.equals(req.getClearParent())) {
            c.setParent(null);
        } else if (req.getParentId() != null) {
            if (Objects.equals(req.getParentId(), id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            Category newParent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.getParentId()));
            Category cursor = newParent;
            int safety = 0;
            while (cursor != null && safety++ < 100) {
                if (Objects.equals(cursor.getId(), id)) {
                    throw new BadRequestException("Cannot move category here: would create a cycle");
                }
                cursor = cursor.getParent();
            }
            c.setParent(newParent);
        }
        if (req.getName() != null && !req.getName().isBlank()) c.setName(req.getName());
        if (req.getImageUrl() != null) c.setImageUrl(blankToNull(req.getImageUrl()));
        if (req.getSortOrder() != null) c.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) c.setIsActive(req.getIsActive());

        if (req.getNameEn() != null) {
            CategoryTranslation existing = translationRepository.findByCategoryIdAndLocale(id, LOCALE_EN).orElse(null);
            if (req.getNameEn().isBlank()) {
                if (existing != null) translationRepository.delete(existing);
            } else if (existing != null) {
                existing.setName(req.getNameEn());
                existing.setTranslatedAt(Instant.now());
            } else {
                translationRepository.save(CategoryTranslation.builder()
                        .category(c).locale(LOCALE_EN).name(req.getNameEn())
                        .translatedAt(Instant.now()).build());
            }
        }
        return get(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (categoryRepository.existsByParentId(id)) {
            throw new BadRequestException("Cannot delete category with sub-categories; delete or move children first");
        }
        long products = productRepository.countByCategoryIdAndDeletedAtIsNull(id);
        if (products > 0) {
            throw new BadRequestException("Cannot delete category with " + products + " product(s); reassign or soft-delete them first");
        }
        translationRepository.deleteByCategoryId(id);
        categoryRepository.delete(c);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
