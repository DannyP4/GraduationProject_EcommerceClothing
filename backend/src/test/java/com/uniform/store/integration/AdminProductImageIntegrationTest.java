package com.uniform.store.integration;

import com.uniform.store.dto.request.CreateProductImageRequest;
import com.uniform.store.dto.request.UpdateProductImageRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.User;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminProductImageIntegrationTest extends BaseIntegrationTest {

    @Autowired private ProductImageRepository imageRepository;
    @MockBean private CloudinaryService cloudinaryService;

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
        mockMvc.perform(get("/admin/products/" + product.getId() + "/images"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(get("/admin/products/" + product.getId() + "/images")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_firstImage_isAutoPrimary() throws Exception {
        CreateProductImageRequest req = new CreateProductImageRequest();
        req.setUrl("https://cdn/test.jpg");
        req.setPublicId("uniform/products/test-1");
        req.setIsPrimary(false);

        mockMvc.perform(post("/admin/products/" + product.getId() + "/images")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPrimary").value(true));
    }

    @Test
    void create_secondImageAsPrimary_demotesFirst() throws Exception {
        data.createProductImage(product, true, "pub-1");

        CreateProductImageRequest req = new CreateProductImageRequest();
        req.setUrl("https://cdn/test2.jpg");
        req.setPublicId("pub-2");
        req.setIsPrimary(true);

        mockMvc.perform(post("/admin/products/" + product.getId() + "/images")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPrimary").value(true));

        List<ProductImage> all = imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(product.getId());
        long primaryCount = all.stream().filter(i -> Boolean.TRUE.equals(i.getIsPrimary())).count();
        assertThat(primaryCount).isEqualTo(1);
    }

    @Test
    void create_atMaxLimit_returns400() throws Exception {
        for (int i = 0; i < 8; i++) {
            data.createProductImage(product, i == 0, "pub-" + i);
        }
        CreateProductImageRequest req = new CreateProductImageRequest();
        req.setUrl("u"); req.setPublicId("would-fail");

        mockMvc.perform(post("/admin/products/" + product.getId() + "/images")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("max")));
    }

    @Test
    void update_setsPrimaryAndDemotesOther() throws Exception {
        ProductImage first = data.createProductImage(product, true, "pub-1");
        ProductImage second = data.createProductImage(product, false, "pub-2");

        UpdateProductImageRequest req = new UpdateProductImageRequest();
        req.setIsPrimary(true);

        mockMvc.perform(put("/admin/images/" + second.getId())
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertThat(imageRepository.findById(first.getId()).orElseThrow().getIsPrimary()).isFalse();
        assertThat(imageRepository.findById(second.getId()).orElseThrow().getIsPrimary()).isTrue();
    }

    @Test
    void delete_callsCloudinaryAndRemovesRow() throws Exception {
        ProductImage img = data.createProductImage(product, true, "uniform/products/sample-abc");

        mockMvc.perform(delete("/admin/images/" + img.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        assertThat(imageRepository.findById(img.getId())).isEmpty();
        verify(cloudinaryService).deleteByPublicId("uniform/products/sample-abc");
    }

    @Test
    void delete_primaryImage_promotesNextRemaining() throws Exception {
        ProductImage primary = data.createProductImage(product, true, "pub-1");
        ProductImage secondary = data.createProductImage(product, false, "pub-2");

        mockMvc.perform(delete("/admin/images/" + primary.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        assertThat(imageRepository.findById(secondary.getId()).orElseThrow().getIsPrimary()).isTrue();
    }

    @Test
    void list_returnsImagesPrimaryFirst() throws Exception {
        data.createProductImage(product, false, "pub-A");
        data.createProductImage(product, true, "pub-B");

        mockMvc.perform(get("/admin/products/" + product.getId() + "/images")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].publicId").value("pub-B"));
    }
}
