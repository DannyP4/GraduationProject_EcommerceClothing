package com.uniform.store.integration;

import com.uniform.store.dto.request.TryOnCreateRequest;
import com.uniform.store.dto.response.CloudinaryUploadResult;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.User;
import com.uniform.store.enums.TryOnStatus;
import com.uniform.store.repository.TryOnJobRepository;
import com.uniform.store.service.CloudinaryService;
import com.uniform.store.service.VirtualTryOnProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TryOnIntegrationTest extends BaseIntegrationTest {

    @MockBean VirtualTryOnProvider provider;
    @MockBean CloudinaryService cloudinaryService;
    @Autowired TryOnJobRepository tryOnJobRepository;

    User buyer;
    String buyerJwt;
    User other;
    String otherJwt;

    Product product; // has a primary image
    Product imagelessProduct;

    @BeforeEach
    void seed() {
        buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        buyerJwt = data.accessTokenFor(buyer.getEmail());
        other = data.createCustomer("other@uniform.test", "Test1234");
        otherJwt = data.accessTokenFor(other.getEmail());

        Brand brand = data.createBrand();
        Category category = data.createCategory();
        product = data.createProduct(brand, category, new BigDecimal("250000"));
        data.createProductImage(product, true, "pid-primary");
        imagelessProduct = data.createProduct(brand, category, new BigDecimal("300000"));

        when(provider.isEnabled()).thenReturn(true);
        when(provider.name()).thenReturn("FAL_FASHN");
        when(provider.submit(any(), any(), any(), any()))
                .thenReturn(new VirtualTryOnProvider.Submission("req-1", "https://queue.test/req/1"));
    }

    private TryOnCreateRequest createRequest(Long productId) {
        TryOnCreateRequest req = new TryOnCreateRequest();
        req.setProductId(productId);
        req.setUserImageUrl("https://cdn.test/user.jpg");
        return req;
    }

    private Long createJob(String jwt, Long productId) throws Exception {
        String body = mockMvc.perform(post("/try-on")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(productId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/try-on")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(product.getId()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_persistsProcessingJob() throws Exception {
        mockMvc.perform(post("/try-on")
                        .header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(product.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.garmentImageUrl").exists());

        assertThat(tryOnJobRepository.count()).isEqualTo(1);
    }

    @Test
    void create_productWithoutImage_returns400() throws Exception {
        mockMvc.perform(post("/try-on")
                        .header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(imagelessProduct.getId()))))
                .andExpect(status().isBadRequest());

        assertThat(tryOnJobRepository.count()).isZero();
    }

    @Test
    void create_missingUserImage_returns400() throws Exception {
        TryOnCreateRequest req = new TryOnCreateRequest();
        req.setProductId(product.getId());

        mockMvc.perform(post("/try-on")
                        .header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void poll_completesAndReturnsResult() throws Exception {
        Long jobId = createJob(buyerJwt, product.getId());

        when(provider.poll(any()))
                .thenReturn(new VirtualTryOnProvider.PollResult(TryOnStatus.SUCCEEDED, "https://fal/result.png", null));
        when(cloudinaryService.uploadImageFromUrl(any(), any(), any()))
                .thenReturn(CloudinaryUploadResult.builder()
                        .secureUrl("https://cdn.test/stored.png").publicId("pid-result").build());

        mockMvc.perform(get("/try-on/" + jobId)
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.resultImageUrl").value("https://cdn.test/stored.png"));

        assertThat(tryOnJobRepository.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(TryOnStatus.SUCCEEDED);
    }

    @Test
    void poll_otherUsersJob_returns404() throws Exception {
        Long jobId = createJob(buyerJwt, product.getId());

        mockMvc.perform(get("/try-on/" + jobId)
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isNotFound());
    }
}
