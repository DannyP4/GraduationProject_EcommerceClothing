package com.uniform.store.integration;

import com.uniform.store.dto.request.CreateBrandRequest;
import com.uniform.store.dto.request.UpdateBrandRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.User;
import com.uniform.store.repository.BrandRepository;
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

class AdminBrandIntegrationTest extends BaseIntegrationTest {

    @Autowired private BrandRepository brandRepository;

    private String adminJwt;
    private String customerJwt;

    @BeforeEach
    void seed() {
        User admin = data.createAdmin("admin@uniform.test", "Admin1234");
        adminJwt = data.accessTokenFor(admin.getEmail());
        User customer = data.createCustomer("user@uniform.test", "Pass1234");
        customerJwt = data.accessTokenFor(customer.getEmail());
    }

    @Test
    void list_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/admin/brands"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(get("/admin/brands").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_minimalPayload_persistsWithTranslations() throws Exception {
        CreateBrandRequest req = new CreateBrandRequest();
        req.setSlug("atlas-studio");
        req.setName("Atlas Studio");
        req.setDescriptionVi("Thương hiệu Atlas");
        req.setDescriptionEn("Atlas brand description");

        mockMvc.perform(post("/admin/brands")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("atlas-studio"))
                .andExpect(jsonPath("$.data.descriptionVi").value("Thương hiệu Atlas"))
                .andExpect(jsonPath("$.data.descriptionEn").value("Atlas brand description"))
                .andExpect(jsonPath("$.data.productCount").value(0));

        assertThat(brandRepository.existsBySlug("atlas-studio")).isTrue();
    }

    @Test
    void create_duplicateSlug_returns400() throws Exception {
        brandRepository.save(Brand.builder().slug("uniform").name("Uniform").isActive(true).build());

        CreateBrandRequest req = new CreateBrandRequest();
        req.setSlug("uniform");
        req.setName("Uniform");

        mockMvc.perform(post("/admin/brands")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("slug already exists")));
    }

    @Test
    void create_invalidSlugFormat_returns400() throws Exception {
        CreateBrandRequest req = new CreateBrandRequest();
        req.setSlug("Brand With Spaces");
        req.setName("Bad");

        mockMvc.perform(post("/admin/brands")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_logoAndWebsite_persists() throws Exception {
        Brand b = brandRepository.save(Brand.builder().slug("riverwear").name("Riverwear").isActive(true).build());

        UpdateBrandRequest req = new UpdateBrandRequest();
        req.setLogoUrl("https://example.com/logo.png");
        req.setWebsiteUrl("https://riverwear.example.com");
        req.setDescriptionEn("Outdoor brand");

        mockMvc.perform(put("/admin/brands/" + b.getId())
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logoUrl").value("https://example.com/logo.png"))
                .andExpect(jsonPath("$.data.descriptionEn").value("Outdoor brand"));
    }

    @Test
    void delete_brandWithProduct_returns400() throws Exception {
        Brand brand = data.createBrand();
        Category category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        assertThat(product.getId()).isNotNull();

        mockMvc.perform(delete("/admin/brands/" + brand.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("product")));
    }

    @Test
    void delete_emptyBrand_succeeds() throws Exception {
        Brand b = brandRepository.save(Brand.builder().slug("halftone").name("Halftone").isActive(true).build());

        mockMvc.perform(delete("/admin/brands/" + b.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        assertThat(brandRepository.findById(b.getId())).isEmpty();
    }

    @Test
    void list_returnsAllBrandsIncludingInactive() throws Exception {
        brandRepository.save(Brand.builder().slug("active-one").name("Active").isActive(true).build());
        brandRepository.save(Brand.builder().slug("inactive-one").name("Inactive").isActive(false).build());

        mockMvc.perform(get("/admin/brands").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
