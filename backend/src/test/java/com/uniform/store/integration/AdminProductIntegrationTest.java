package com.uniform.store.integration;

import com.uniform.store.dto.request.CreateProductRequest;
import com.uniform.store.dto.request.UpdateProductRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.User;
import com.uniform.store.enums.Gender;
import com.uniform.store.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminProductIntegrationTest extends BaseIntegrationTest {

    @Autowired private ProductRepository productRepository;

    private String adminJwt;
    private String customerJwt;
    private Brand brand;
    private Category category;

    @BeforeEach
    void seed() {
        User admin = data.createAdmin("admin@uniform.test", "Admin1234");
        adminJwt = data.accessTokenFor(admin.getEmail());
        User customer = data.createCustomer("user@uniform.test", "Pass1234");
        customerJwt = data.accessTokenFor(customer.getEmail());
        brand = data.createBrand();
        category = data.createCategory();
    }

    @Test
    void list_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(get("/admin/products").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_validRequest_returns201WithDetail() throws Exception {
        CreateProductRequest req = baseCreate("new-tee");

        mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("new-tee"))
                .andExpect(jsonPath("$.data.brand.id").value(brand.getId()))
                .andExpect(jsonPath("$.data.category.id").value(category.getId()))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.variants.length()").value(0))
                .andExpect(jsonPath("$.data.images.length()").value(0));

        assertThat(productRepository.existsBySlug("new-tee")).isTrue();
    }

    @Test
    void create_duplicateSlug_returns400() throws Exception {
        data.createProduct(brand, category, new BigDecimal("100000"));
        CreateProductRequest req = baseCreate("essential-tee-1");

        // Force a deterministic slug collision by saving with the same slug first
        productRepository.save(Product.builder()
                .brand(brand).category(category)
                .slug("dup-slug").name("Dup")
                .gender(Gender.UNISEX).basePrice(new BigDecimal("1")).currency("VND")
                .isActive(true).build());

        req.setSlug("dup-slug");
        mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("slug already exists")));
    }

    @Test
    void create_invalidSlug_returns400() throws Exception {
        CreateProductRequest req = baseCreate("Has Spaces");
        mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unknownBrand_returns404() throws Exception {
        CreateProductRequest req = baseCreate("new-tee");
        req.setBrandId(999_999L);

        mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_changesNameAndPrice() throws Exception {
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        UpdateProductRequest req = new UpdateProductRequest();
        req.setName("Renamed");
        req.setBasePrice(new BigDecimal("300000"));

        mockMvc.perform(put("/admin/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed"))
                .andExpect(jsonPath("$.data.basePrice").value(300000));
    }

    @Test
    void softDelete_setsDeletedAtAndDeactivates() throws Exception {
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));

        mockMvc.perform(delete("/admin/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isNotNull();
        assertThat(reloaded.getIsActive()).isFalse();
    }

    @Test
    void softDelete_thenRestore_clearsDeletedAt() throws Exception {
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));

        mockMvc.perform(delete("/admin/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/products/" + product.getId() + "/restore")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedAt").doesNotExist());
    }

    @Test
    void list_defaultExcludesDeleted_onlyShowsDeletedWhenRequested() throws Exception {
        Product alive = data.createProduct(brand, category, new BigDecimal("100"));
        Product dead = data.createProduct(brand, category, new BigDecimal("100"));
        mockMvc.perform(delete("/admin/products/" + dead.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/products")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(alive.getId()));

        mockMvc.perform(get("/admin/products?deleted=only")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(dead.getId()));

        mockMvc.perform(get("/admin/products?deleted=both")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void list_searchFilter_matchesNameSubstring() throws Exception {
        productRepository.save(Product.builder()
                .brand(brand).category(category)
                .slug("alpha-shirt").name("Alpha Shirt")
                .gender(Gender.UNISEX).basePrice(new BigDecimal("100")).currency("VND")
                .isActive(true).build());
        productRepository.save(Product.builder()
                .brand(brand).category(category)
                .slug("beta-hoodie").name("Beta Hoodie")
                .gender(Gender.UNISEX).basePrice(new BigDecimal("100")).currency("VND")
                .isActive(true).build());

        mockMvc.perform(get("/admin/products?search=alpha")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].slug").value("alpha-shirt"));
    }

    @Test
    void list_invalidDeletedParam_returns400() throws Exception {
        mockMvc.perform(get("/admin/products?deleted=bogus")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/admin/products/999999")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void hardDelete_aliveProduct_returns400() throws Exception {
        Product product = data.createProduct(brand, category, new BigDecimal("100000"));

        mockMvc.perform(delete("/admin/products/" + product.getId() + "/permanent")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("soft-deleted first")));

        assertThat(productRepository.existsById(product.getId())).isTrue();
    }

    @Test
    void hardDelete_softDeletedProduct_removesRow() throws Exception {
        Product product = data.createProduct(brand, category, new BigDecimal("100000"));

        mockMvc.perform(delete("/admin/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/admin/products/" + product.getId() + "/permanent")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        assertThat(productRepository.existsById(product.getId())).isFalse();
    }

    @Test
    void hardDelete_variantReferencedByOrder_returns400() throws Exception {
        Product product = data.createProduct(brand, category, new BigDecimal("100000"));
        com.uniform.store.entity.ProductVariant variant = data.createVariant(product, 5);

        User customer = data.createCustomer("buyer@uniform.test", "Pass1234");
        jdbc.update("""
                INSERT INTO orders (order_number, user_id, status, subtotal, discount_total, shipping_cost,
                                    tax_total, grand_total, currency, shipping_recipient, shipping_phone,
                                    shipping_line1, shipping_district, shipping_city, shipping_country, placed_at)
                VALUES ('TEST-HD-001', ?, 'PAID', 100, 0, 0, 0, 100, 'VND', 'X', '0900000000',
                        'L1', 'D1', 'HCM', 'VN', NOW())
                """, customer.getId());
        Long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE order_number='TEST-HD-001'", Long.class);
        jdbc.update("""
                INSERT INTO order_items (order_id, variant_id, product_name, variant_label, sku,
                                         unit_price, quantity, line_total)
                VALUES (?, ?, 'P', 'M / Black', ?, 100, 1, 100)
                """, orderId, variant.getId(), variant.getSku());

        mockMvc.perform(delete("/admin/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/admin/products/" + product.getId() + "/permanent")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("referenced by historical orders")));

        assertThat(productRepository.existsById(product.getId())).isTrue();
    }

    private CreateProductRequest baseCreate(String slug) {
        CreateProductRequest req = new CreateProductRequest();
        req.setSlug(slug);
        req.setName("Test Product " + slug);
        req.setBrandId(brand.getId());
        req.setCategoryId(category.getId());
        req.setGender(Gender.UNISEX);
        req.setBasePrice(new BigDecimal("250000"));
        return req;
    }
}
