package com.uniform.store.integration;

import com.uniform.store.dto.request.CreateVariantRequest;
import com.uniform.store.dto.request.UpdateVariantRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.repository.ProductVariantRepository;
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

class AdminVariantIntegrationTest extends BaseIntegrationTest {

    @Autowired private ProductVariantRepository variantRepository;

    private String adminJwt;
    private String customerJwt;
    private Brand brand;
    private Category category;
    private Product product;

    @BeforeEach
    void seed() {
        User admin = data.createAdmin("admin@uniform.test", "Admin1234");
        adminJwt = data.accessTokenFor(admin.getEmail());
        User customer = data.createCustomer("user@uniform.test", "Pass1234");
        customerJwt = data.accessTokenFor(customer.getEmail());
        brand = data.createBrand();
        category = data.createCategory();
        product = data.createProduct(brand, category, new BigDecimal("250000"));
    }

    @Test
    void list_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/admin/products/" + product.getId() + "/variants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(get("/admin/products/" + product.getId() + "/variants")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_valid_returns201() throws Exception {
        CreateVariantRequest req = newVariantRequest("SKU-A1", "M", "Black");
        req.setColorHex("#000000");
        req.setStockQuantity(15);

        mockMvc.perform(post("/admin/products/" + product.getId() + "/variants")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku").value("SKU-A1"))
                .andExpect(jsonPath("$.data.size").value("M"))
                .andExpect(jsonPath("$.data.color").value("Black"))
                .andExpect(jsonPath("$.data.stockQuantity").value(15));
    }

    @Test
    void create_duplicateSku_returns400() throws Exception {
        data.createVariant(product, 5);
        ProductVariant existing = variantRepository.findByProductIdOrderBySizeAscColorAsc(product.getId()).get(0);

        CreateVariantRequest req = newVariantRequest(existing.getSku(), "L", "Red");
        mockMvc.perform(post("/admin/products/" + product.getId() + "/variants")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SKU already exists")));
    }

    @Test
    void create_duplicateSizeColor_returns400() throws Exception {
        data.createVariant(product, 5);
        CreateVariantRequest req = newVariantRequest("SKU-NEW", "M", "Black");

        mockMvc.perform(post("/admin/products/" + product.getId() + "/variants")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("size 'M' and color 'Black'")));
    }

    @Test
    void create_invalidColorHex_returns400() throws Exception {
        CreateVariantRequest req = newVariantRequest("SKU-Z1", "S", "Red");
        req.setColorHex("not-a-hex");

        mockMvc.perform(post("/admin/products/" + product.getId() + "/variants")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_changesStock() throws Exception {
        data.createVariant(product, 5);
        ProductVariant existing = variantRepository.findByProductIdOrderBySizeAscColorAsc(product.getId()).get(0);

        UpdateVariantRequest req = new UpdateVariantRequest();
        req.setStockQuantity(42);
        mockMvc.perform(put("/admin/variants/" + existing.getId())
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(42));
    }

    @Test
    void delete_referencedByOrderItem_returns400() throws Exception {
        data.createVariant(product, 5);
        ProductVariant existing = variantRepository.findByProductIdOrderBySizeAscColorAsc(product.getId()).get(0);

        User customer = data.createCustomer("buyer@uniform.test", "Pass1234");
        jdbc.update("""
                INSERT INTO orders (order_number, user_id, status, subtotal, discount_total, shipping_cost,
                                    tax_total, grand_total, currency, shipping_recipient, shipping_phone,
                                    shipping_line1, shipping_district, shipping_city, shipping_country, placed_at)
                VALUES ('TEST-001', ?, 'PAID', 100, 0, 0, 0, 100, 'VND', 'X', '0900000000',
                        'L1', 'D1', 'HCM', 'VN', NOW())
                """, customer.getId());
        Long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE order_number='TEST-001'", Long.class);
        jdbc.update("""
                INSERT INTO order_items (order_id, variant_id, product_name, variant_label, sku,
                                         unit_price, quantity, line_total)
                VALUES (?, ?, 'P', 'M / Black', ?, 100, 1, 100)
                """, orderId, existing.getId(), existing.getSku());

        mockMvc.perform(delete("/admin/variants/" + existing.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("referenced by existing orders")));

        assertThat(variantRepository.existsById(existing.getId())).isTrue();
    }

    @Test
    void delete_orphanVariant_succeeds() throws Exception {
        data.createVariant(product, 5);
        ProductVariant existing = variantRepository.findByProductIdOrderBySizeAscColorAsc(product.getId()).get(0);

        mockMvc.perform(delete("/admin/variants/" + existing.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        assertThat(variantRepository.existsById(existing.getId())).isFalse();
    }

    @Test
    void list_returnsAllVariantsIncludingInactive() throws Exception {
        data.createVariant(product, 5);
        ProductVariant existing = variantRepository.findByProductIdOrderBySizeAscColorAsc(product.getId()).get(0);
        existing.setIsActive(false);
        variantRepository.save(existing);

        mockMvc.perform(get("/admin/products/" + product.getId() + "/variants")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].isActive").value(false));
    }

    private static CreateVariantRequest newVariantRequest(String sku, String size, String color) {
        CreateVariantRequest req = new CreateVariantRequest();
        req.setSku(sku);
        req.setSize(size);
        req.setColor(color);
        req.setStockQuantity(0);
        req.setIsActive(true);
        return req;
    }
}
