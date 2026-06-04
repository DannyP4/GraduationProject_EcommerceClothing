package com.uniform.store.integration;

import com.uniform.store.bootstrap.CatalogSeedProperties;
import com.uniform.store.bootstrap.CatalogSeedRunner;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSeedRunnerIntegrationTest extends BaseIntegrationTest {

    @Autowired CatalogSeedRunner runner;
    @Autowired CatalogSeedProperties props;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired ProductImageRepository imageRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired BrandRepository brandRepository;

    @Test
    void loadsBundledCatalogWhenEmpty() {
        props.setEnabled(true);
        assertThat(productRepository.count()).isZero();

        runner.run();

        long products = productRepository.count();
        assertThat(products).isGreaterThan(900);
        assertThat(variantRepository.count()).isGreaterThan(products);
        assertThat(imageRepository.count()).isGreaterThanOrEqualTo(products);
        assertThat(categoryRepository.count()).isGreaterThan(20);
        assertThat(brandRepository.count()).isGreaterThan(0);
    }

    @Test
    void skipsWhenProductsAlreadyExist() {
        props.setEnabled(true);
        runner.run();
        long after1 = productRepository.count();

        runner.run();
        assertThat(productRepository.count()).isEqualTo(after1);
    }

    @Test
    void skipsWhenDisabled() {
        props.setEnabled(false);
        runner.run();
        assertThat(productRepository.count()).isZero();
    }
}
