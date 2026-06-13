package com.uniform.store.service.impl;

import com.uniform.store.dto.response.AutoTranslateReport;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.BrandTranslation;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.CategoryTranslation;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductTranslation;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.TranslationQuotaException;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.BrandTranslationRepository;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.CategoryTranslationRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductTranslationRepository;
import com.uniform.store.service.CatalogTranslationService;
import com.uniform.store.service.TranslationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogTranslationServiceImpl implements CatalogTranslationService {

    private static final Set<String> ALLOWED = Set.of("vi", "ja");
    private static final String LOCALE_EN = "en";
    private static final int PAGE_SIZE = 200;
    private static final int PRODUCT_CHUNK = 20;   // up to ~40 texts/call
    private static final int SIMPLE_CHUNK = 40;    // categories/brands: 1 text each

    private final ProductRepository productRepository;
    private final ProductTranslationRepository productTranslationRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final BrandRepository brandRepository;
    private final BrandTranslationRepository brandTranslationRepository;
    private final TranslationProvider translationProvider;

    @Override
    public AutoTranslateReport run(String targetLocale, Integer limitProducts, Long maxChars) {
        String locale = targetLocale == null ? "" : targetLocale.toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(locale)) {
            throw new BadRequestException("Target locale must be 'vi' or 'ja' (base catalog is English)");
        }
        if (!translationProvider.isEnabled()) {
            throw new BadRequestException("DeepL is not configured (set DEEPL_API_KEY)");
        }
        long budget = (maxChars == null || maxChars <= 0) ? Long.MAX_VALUE : maxChars;

        Counters c = new Counters();
        try {
            translateProducts(locale, limitProducts, budget, c);
            translateCategories(locale, budget, c);
            translateBrands(locale, budget, c);
        } catch (TranslationQuotaException ex) {
            c.stoppedEarly = true;
            c.note = "Stopped early: " + ex.getMessage();
            log.warn("Auto-translate [{}] stopped early: {}", locale, ex.getMessage());
        }

        log.info("Auto-translate [{}]: products={} categories={} brands={} skipped={} chars={} stoppedEarly={}",
                locale, c.products, c.categories, c.brands, c.skipped, c.chars, c.stoppedEarly);
        return new AutoTranslateReport(locale, c.products, c.categories, c.brands, c.skipped, c.chars,
                c.stoppedEarly, c.note != null ? c.note : "OK");
    }

    private void translateProducts(String locale, Integer limit, long budget, Counters c) {
        int max = (limit == null || limit <= 0) ? Integer.MAX_VALUE : limit;
        int processed = 0;
        int page = 0;
        while (processed < max) {
            Page<Product> pg = productRepository.findAll(PageRequest.of(page, PAGE_SIZE, Sort.by("id")));
            if (pg.isEmpty()) break;

            List<Long> ids = pg.getContent().stream().map(Product::getId).toList();
            Set<Long> have = new HashSet<>();
            for (ProductTranslation t : productTranslationRepository.findByProductIdInAndLocale(ids, locale)) {
                have.add(t.getProduct().getId());
            }

            List<Product> pending = new ArrayList<>();
            for (Product p : pg.getContent()) {
                if (have.contains(p.getId())) { c.skipped++; continue; }
                pending.add(p);
                processed++;
                if (processed >= max) break;
            }

            for (int i = 0; i < pending.size(); i += PRODUCT_CHUNK) {
                translateProductChunk(pending.subList(i, Math.min(i + PRODUCT_CHUNK, pending.size())), locale, budget, c);
            }

            if (!pg.hasNext()) break;
            page++;
        }
    }

    private void translateProductChunk(List<Product> chunk, String locale, long budget, Counters c) {
        List<String> texts = new ArrayList<>();
        int[] nameIdx = new int[chunk.size()];
        int[] descIdx = new int[chunk.size()];
        long chunkChars = 0;
        for (int i = 0; i < chunk.size(); i++) {
            Product p = chunk.get(i);
            nameIdx[i] = texts.size();
            texts.add(p.getName());
            chunkChars += length(p.getName());
            if (hasText(p.getDescription())) {
                descIdx[i] = texts.size();
                texts.add(p.getDescription());
                chunkChars += length(p.getDescription());
            } else {
                descIdx[i] = -1;
            }
        }
        if (c.chars + chunkChars > budget) {
            throw new TranslationQuotaException("character budget reached (" + budget + ")");
        }

        List<String> out = translationProvider.translate(texts, locale);
        c.chars += chunkChars;

        List<ProductTranslation> rows = new ArrayList<>(chunk.size());
        for (int i = 0; i < chunk.size(); i++) {
            Product p = chunk.get(i);
            rows.add(ProductTranslation.builder()
                    .product(p)
                    .locale(locale)
                    .name(pick(out, nameIdx[i], p.getName()))
                    .description(descIdx[i] >= 0 ? pick(out, descIdx[i], null) : null)
                    .isAutoTranslated(true)
                    .translatedAt(Instant.now())
                    .build());
        }
        productTranslationRepository.saveAll(rows);
        c.products += rows.size();
    }

    private void translateCategories(String locale, long budget, Counters c) {
        List<Category> all = categoryRepository.findAll();
        if (all.isEmpty()) return;

        List<Long> ids = all.stream().map(Category::getId).toList();
        Set<Long> have = new HashSet<>();
        for (CategoryTranslation t : categoryTranslationRepository.findByCategoryIdInAndLocale(ids, locale)) {
            have.add(t.getCategory().getId());
        }
        List<Category> pending = all.stream().filter(x -> !have.contains(x.getId())).toList();
        c.skipped += (all.size() - pending.size());

        for (int i = 0; i < pending.size(); i += SIMPLE_CHUNK) {
            List<Category> chunk = pending.subList(i, Math.min(i + SIMPLE_CHUNK, pending.size()));
            List<String> names = chunk.stream().map(Category::getName).toList();
            long chunkChars = names.stream().mapToLong(CatalogTranslationServiceImpl::length).sum();
            if (c.chars + chunkChars > budget) {
                throw new TranslationQuotaException("character budget reached (" + budget + ")");
            }
            List<String> out = translationProvider.translate(names, locale);
            c.chars += chunkChars;

            List<CategoryTranslation> rows = new ArrayList<>(chunk.size());
            for (int j = 0; j < chunk.size(); j++) {
                rows.add(CategoryTranslation.builder()
                        .category(chunk.get(j))
                        .locale(locale)
                        .name(pick(out, j, chunk.get(j).getName()))
                        .isAutoTranslated(true)
                        .translatedAt(Instant.now())
                        .build());
            }
            categoryTranslationRepository.saveAll(rows);
            c.categories += rows.size();
        }
    }

    private void translateBrands(String locale, long budget, Counters c) {
        List<Brand> all = brandRepository.findAll();
        if (all.isEmpty()) return;

        List<Long> ids = all.stream().map(Brand::getId).toList();
        Map<Long, String> enDesc = new HashMap<>();
        for (BrandTranslation t : brandTranslationRepository.findByBrandIdInAndLocale(ids, LOCALE_EN)) {
            if (hasText(t.getDescription())) enDesc.put(t.getBrand().getId(), t.getDescription());
        }
        Set<Long> have = new HashSet<>();
        for (BrandTranslation t : brandTranslationRepository.findByBrandIdInAndLocale(ids, locale)) {
            have.add(t.getBrand().getId());
        }
        // Only brands that have an English description to translate from and lack the target locale.
        List<Brand> pending = all.stream()
                .filter(b -> enDesc.containsKey(b.getId()) && !have.contains(b.getId()))
                .toList();

        for (int i = 0; i < pending.size(); i += SIMPLE_CHUNK) {
            List<Brand> chunk = pending.subList(i, Math.min(i + SIMPLE_CHUNK, pending.size()));
            List<String> srcs = chunk.stream().map(b -> enDesc.get(b.getId())).toList();
            long chunkChars = srcs.stream().mapToLong(CatalogTranslationServiceImpl::length).sum();
            if (c.chars + chunkChars > budget) {
                throw new TranslationQuotaException("character budget reached (" + budget + ")");
            }
            List<String> out = translationProvider.translate(srcs, locale);
            c.chars += chunkChars;

            List<BrandTranslation> rows = new ArrayList<>(chunk.size());
            for (int j = 0; j < chunk.size(); j++) {
                rows.add(BrandTranslation.builder()
                        .brand(chunk.get(j))
                        .locale(locale)
                        .description(pick(out, j, null))
                        .isAutoTranslated(true)
                        .translatedAt(Instant.now())
                        .build());
            }
            brandTranslationRepository.saveAll(rows);
            c.brands += rows.size();
        }
    }

    private static String pick(List<String> list, int idx, String fallback) {
        if (idx >= 0 && idx < list.size()) {
            String v = list.get(idx);
            if (v != null && !v.isBlank()) return v;
        }
        return fallback;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static long length(String s) {
        return s == null ? 0 : s.length();
    }

    private static final class Counters {
        int products;
        int categories;
        int brands;
        int skipped;
        long chars;
        boolean stoppedEarly;
        String note;
    }
}
