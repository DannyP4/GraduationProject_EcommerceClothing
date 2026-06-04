package com.uniform.store.bootstrap;

import com.uniform.store.bootstrap.KaggleCatalogMapper.Row;
import com.uniform.store.dto.response.CloudinaryUploadResult;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Order(30)
@RequiredArgsConstructor
public class KaggleCatalogImporter implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KaggleCatalogImporter.class);

    private static final String[] BRAND_POOL = {
            "Roadster", "HRX", "Nike", "Puma", "Levis", "Wrangler",
            "Allen Solly", "Peter England", "United Colors of Benetton", "Mango"};

    private final KaggleImportProperties props;
    private final KaggleCatalogMapper mapper;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CloudinaryService cloudinaryService;
    private final PlatformTransactionManager txManager;

    @Override
    public void run(String... args) {
        if (!props.isEnabled()) return;

        Path root = Path.of(props.getPath());
        Path csv = root.resolve("styles.csv");
        Path imagesDir = root.resolve("images");
        if (!Files.isRegularFile(csv)) {
            log.error("Kaggle import: styles.csv not found at {}", csv);
            return;
        }

        log.info("Kaggle import starting from {} (limit={})", root, props.getLimit());
        List<Row> selected = selectSubset(csv, imagesDir, props.getLimit());
        log.info("Kaggle import: {} rows selected", selected.size());

        TransactionTemplate tx = new TransactionTemplate(txManager);
        Map<String, Category> categoryCache = cacheBySlug(categoryRepository.findAll(), Category::getSlug);
        Map<String, Brand> brandCache = cacheBySlug(brandRepository.findAll(), Brand::getSlug);

        int imported = 0, skipped = 0, failed = 0;
        for (Row r : selected) {
            String slug = mapper.slugForProduct(r);
            if (productRepository.existsBySlug(slug)) {
                skipped++;
                continue;
            }
            try {
                byte[] bytes = Files.readAllBytes(imagesDir.resolve(r.id() + ".jpg"));
                CloudinaryUploadResult upload = cloudinaryService.uploadImage(bytes, "kaggle-" + r.id());
                Category category = categoryCache.computeIfAbsent(mapper.slugify(r.articleType()),
                        s -> categoryRepository.save(Category.builder()
                                .slug(s).name(r.articleType()).parent(null).sortOrder(0).isActive(true).build()));
                Brand brand = brandFor(r, brandCache);
                tx.executeWithoutResult(st -> persist(r, slug, category, brand, upload));
                imported++;
                if (imported % 50 == 0) log.info("Kaggle import: {} products imported...", imported);
            } catch (Exception e) {
                failed++;
                log.warn("Kaggle import: skipped id={} ({})", r.id(), e.getMessage());
            }
        }
        log.info("Kaggle import done: imported={}, skipped(existing)={}, failed={}", imported, skipped, failed);
    }

    private void persist(Row r, String slug, Category category, Brand brand, CloudinaryUploadResult upload) {
        Product product = productRepository.save(Product.builder()
                .brand(brand)
                .category(category)
                .slug(slug)
                .name(r.productDisplayName())
                .description(mapper.description(r))
                .gender(mapper.genderOf(r.gender()))
                .basePrice(mapper.priceFor(r.subCategory(), r.id()))
                .currency("VND")
                .isActive(true)
                .publishedAt(Instant.now())
                .build());

        imageRepository.save(ProductImage.builder()
                .product(product)
                .url(upload.getSecureUrl())
                .publicId(upload.getPublicId())
                .altText(r.productDisplayName())
                .sortOrder(0)
                .isPrimary(true)
                .build());

        String color = r.baseColour().isBlank() ? "Default" : r.baseColour();
        String hex = mapper.colorHex(r.baseColour());
        for (String size : mapper.sizesFor(r.subCategory())) {
            variantRepository.save(ProductVariant.builder()
                    .product(product)
                    .sku(mapper.skuFor(r.id(), size))
                    .size(size)
                    .color(color)
                    .colorHex(hex)
                    .stockQuantity(mapper.stockFor(r.id()))
                    .isActive(true)
                    .build());
        }
    }

    private List<Row> selectSubset(Path csv, Path imagesDir, int limit) {
        LinkedHashMap<String, ArrayDeque<Row>> byType = new LinkedHashMap<>();
        try (BufferedReader br = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                Optional<Row> parsed = mapper.parseLine(line);
                if (parsed.isEmpty()) continue;
                Row r = parsed.get();
                if (!mapper.isWearable(r)) continue;
                if (!Files.isRegularFile(imagesDir.resolve(r.id() + ".jpg"))) continue;
                byType.computeIfAbsent(r.articleType(), k -> new ArrayDeque<>()).add(r);
            }
        } catch (IOException e) {
            log.error("Kaggle import: failed reading {}: {}", csv, e.getMessage());
            return List.of();
        }

        List<Row> out = new ArrayList<>(limit);
        boolean progress = true;
        while (out.size() < limit && progress) {
            progress = false;
            for (ArrayDeque<Row> queue : byType.values()) {
                if (out.size() >= limit) break;
                Row r = queue.poll();
                if (r != null) {
                    out.add(r);
                    progress = true;
                }
            }
        }
        return out;
    }

    private Brand brandFor(Row r, Map<String, Brand> cache) {
        String name = BRAND_POOL[(int) Math.floorMod(r.id(), BRAND_POOL.length)];
        String slug = mapper.slugify(name);
        return cache.computeIfAbsent(slug, s -> brandRepository.save(Brand.builder()
                .slug(s).name(name).websiteUrl("https://example.com/" + s).isActive(true).build()));
    }

    private <T> Map<String, T> cacheBySlug(List<T> items, java.util.function.Function<T, String> slug) {
        Map<String, T> map = new HashMap<>();
        for (T item : items) map.put(slug.apply(item), item);
        return map;
    }
}
