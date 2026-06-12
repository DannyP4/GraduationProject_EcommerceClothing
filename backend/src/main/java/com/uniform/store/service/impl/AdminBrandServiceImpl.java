package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateBrandRequest;
import com.uniform.store.dto.request.UpdateBrandRequest;
import com.uniform.store.dto.response.AdminBrandDto;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.BrandTranslation;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.BrandTranslationRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.service.AdminBrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBrandServiceImpl implements AdminBrandService {

    private static final String LOCALE_VI = "vi";
    private static final String LOCALE_EN = "en";
    private static final String LOCALE_JA = "ja";

    private final BrandRepository brandRepository;
    private final BrandTranslationRepository translationRepository;
    private final ProductRepository productRepository;

    @Override
    public List<AdminBrandDto> listAll() {
        List<Brand> brands = brandRepository.findAllByOrderByNameAsc();
        if (brands.isEmpty()) return List.of();

        List<Long> ids = brands.stream().map(Brand::getId).toList();
        Map<Long, String> viDesc = new HashMap<>();
        Map<Long, String> enDesc = new HashMap<>();
        Map<Long, String> jaDesc = new HashMap<>();
        for (BrandTranslation t : translationRepository.findByBrandIdIn(ids)) {
            if (LOCALE_VI.equals(t.getLocale())) viDesc.put(t.getBrand().getId(), t.getDescription());
            else if (LOCALE_EN.equals(t.getLocale())) enDesc.put(t.getBrand().getId(), t.getDescription());
            else if (LOCALE_JA.equals(t.getLocale())) jaDesc.put(t.getBrand().getId(), t.getDescription());
        }
        Map<Long, Long> productCounts = new HashMap<>();
        for (Brand b : brands) {
            productCounts.put(b.getId(), productRepository.countByBrandIdAndDeletedAtIsNull(b.getId()));
        }

        return brands.stream()
                .map(b -> toDto(b, viDesc.get(b.getId()), enDesc.get(b.getId()), jaDesc.get(b.getId()), productCounts.get(b.getId())))
                .toList();
    }

    @Override
    public AdminBrandDto get(Long id) {
        Brand b = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
        String vi = translationRepository.findByBrandIdAndLocale(id, LOCALE_VI)
                .map(BrandTranslation::getDescription).orElse(null);
        String en = translationRepository.findByBrandIdAndLocale(id, LOCALE_EN)
                .map(BrandTranslation::getDescription).orElse(null);
        String ja = translationRepository.findByBrandIdAndLocale(id, LOCALE_JA)
                .map(BrandTranslation::getDescription).orElse(null);
        long products = productRepository.countByBrandIdAndDeletedAtIsNull(id);
        return toDto(b, vi, en, ja, products);
    }

    @Override
    @Transactional
    public AdminBrandDto create(CreateBrandRequest req) {
        if (brandRepository.existsBySlug(req.getSlug())) {
            throw new BadRequestException("Brand slug already exists: " + req.getSlug());
        }
        Brand saved = brandRepository.save(Brand.builder()
                .slug(req.getSlug())
                .name(req.getName())
                .logoUrl(blankToNull(req.getLogoUrl()))
                .websiteUrl(blankToNull(req.getWebsiteUrl()))
                .isActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive())
                .build());
        upsertTranslation(saved, LOCALE_VI, req.getDescriptionVi());
        upsertTranslation(saved, LOCALE_EN, req.getDescriptionEn());
        upsertTranslation(saved, LOCALE_JA, req.getDescriptionJa());
        return get(saved.getId());
    }

    @Override
    @Transactional
    public AdminBrandDto update(Long id, UpdateBrandRequest req) {
        Brand b = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id));

        if (req.getName() != null && !req.getName().isBlank()) b.setName(req.getName());
        if (req.getLogoUrl() != null) b.setLogoUrl(blankToNull(req.getLogoUrl()));
        if (req.getWebsiteUrl() != null) b.setWebsiteUrl(blankToNull(req.getWebsiteUrl()));
        if (req.getIsActive() != null) b.setIsActive(req.getIsActive());

        if (req.getDescriptionVi() != null) upsertTranslation(b, LOCALE_VI, req.getDescriptionVi());
        if (req.getDescriptionEn() != null) upsertTranslation(b, LOCALE_EN, req.getDescriptionEn());
        if (req.getDescriptionJa() != null) upsertTranslation(b, LOCALE_JA, req.getDescriptionJa());

        return get(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Brand b = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
        long products = productRepository.countByBrandIdAndDeletedAtIsNull(id);
        if (products > 0) {
            throw new BadRequestException("Cannot delete brand with " + products + " product(s); reassign or soft-delete them first");
        }
        translationRepository.deleteByBrandId(id);
        brandRepository.delete(b);
    }

    private void upsertTranslation(Brand brand, String locale, String description) {
        BrandTranslation existing = translationRepository.findByBrandIdAndLocale(brand.getId(), locale).orElse(null);
        if (description == null || description.isBlank()) {
            if (existing != null) translationRepository.delete(existing);
            return;
        }
        if (existing != null) {
            existing.setDescription(description);
            existing.setTranslatedAt(Instant.now());
        } else {
            translationRepository.save(BrandTranslation.builder()
                    .brand(brand).locale(locale).description(description)
                    .translatedAt(Instant.now()).build());
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static AdminBrandDto toDto(Brand b, String vi, String en, String ja, Long productCount) {
        return AdminBrandDto.builder()
                .id(b.getId())
                .slug(b.getSlug())
                .name(b.getName())
                .logoUrl(b.getLogoUrl())
                .websiteUrl(b.getWebsiteUrl())
                .isActive(b.getIsActive())
                .descriptionVi(vi)
                .descriptionEn(en)
                .descriptionJa(ja)
                .productCount(productCount)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
