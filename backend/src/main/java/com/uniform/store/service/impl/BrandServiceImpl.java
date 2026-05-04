package com.uniform.store.service.impl;

import com.uniform.store.dto.response.BrandDto;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.BrandTranslation;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.BrandTranslationRepository;
import com.uniform.store.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandTranslationRepository brandTranslationRepository;

    @Override
    public List<BrandDto> listBrands(String locale) {
        List<Brand> brands = brandRepository.findByIsActiveTrueOrderByNameAsc();
        if (brands.isEmpty()) {
            return List.of();
        }

        Map<Long, BrandTranslation> translations = brandTranslationRepository
                .findByBrandIdInAndLocale(
                        brands.stream().map(Brand::getId).toList(),
                        locale)
                .stream()
                .collect(Collectors.toMap(t -> t.getBrand().getId(), Function.identity(), (a, b) -> a));

        return brands.stream()
                .map(b -> BrandDto.builder()
                        .id(b.getId())
                        .slug(b.getSlug())
                        .name(b.getName())
                        .description(translations.containsKey(b.getId())
                                ? translations.get(b.getId()).getDescription()
                                : null)
                        .logoUrl(b.getLogoUrl())
                        .websiteUrl(b.getWebsiteUrl())
                        .build())
                .toList();
    }
}
