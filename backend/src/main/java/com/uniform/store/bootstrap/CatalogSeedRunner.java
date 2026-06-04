package com.uniform.store.bootstrap;

import com.uniform.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

// loads 2000+ products into the catalog
@Component
@Order(15)
@RequiredArgsConstructor
public class CatalogSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeedRunner.class);
    private static final String SEED_PATH = "db/seed/catalog_seed.sql";

    private final CatalogSeedProperties props;
    private final ProductRepository productRepository;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        if (!props.isEnabled()) return;
        if (productRepository.count() > 0) {
            log.info("Catalog seed skipped: products already present ({}).", productRepository.count());
            return;
        }
        Resource sql = new ClassPathResource(SEED_PATH);
        if (!sql.exists()) {
            log.info("Catalog seed skipped: {} not on classpath.", SEED_PATH);
            return;
        }
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, sql);
            log.info("Catalog seed loaded from {}: {} products now present.", SEED_PATH, productRepository.count());
        } catch (Exception e) {
            log.error("Catalog seed failed from {}: {}", SEED_PATH, e.getMessage());
        }
    }
}
