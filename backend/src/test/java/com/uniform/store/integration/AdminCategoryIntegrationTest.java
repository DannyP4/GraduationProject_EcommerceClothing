package com.uniform.store.integration;

import com.uniform.store.dto.request.CreateCategoryRequest;
import com.uniform.store.dto.request.UpdateCategoryRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.User;
import com.uniform.store.repository.CategoryRepository;
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

class AdminCategoryIntegrationTest extends BaseIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;

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
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(get("/admin/categories").header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_minimalPayload_persists() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setSlug("tees");
        req.setName("T-Shirts");
        req.setNameVi("Áo thun");

        mockMvc.perform(post("/admin/categories")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("tees"))
                .andExpect(jsonPath("$.data.name").value("T-Shirts"))
                .andExpect(jsonPath("$.data.nameVi").value("Áo thun"))
                .andExpect(jsonPath("$.data.productCount").value(0));

        assertThat(categoryRepository.existsBySlug("tees")).isTrue();
    }

    @Test
    void create_duplicateSlug_returns400() throws Exception {
        categoryRepository.save(Category.builder().slug("hoodies").name("Hoodies").sortOrder(0).isActive(true).build());

        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setSlug("hoodies");
        req.setName("Hoodies");

        mockMvc.perform(post("/admin/categories")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("slug already exists")));
    }

    @Test
    void create_invalidSlugFormat_returns400() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setSlug("Tees With Spaces");
        req.setName("Tees");

        mockMvc.perform(post("/admin/categories")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_setsViTranslation_andClearsWhenBlank() throws Exception {
        Category c = categoryRepository.save(Category.builder()
                .slug("jackets").name("Jackets").sortOrder(0).isActive(true).build());

        UpdateCategoryRequest first = new UpdateCategoryRequest();
        first.setNameVi("Áo khoác");
        mockMvc.perform(put("/admin/categories/" + c.getId())
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nameVi").value("Áo khoác"));

        UpdateCategoryRequest clear = new UpdateCategoryRequest();
        clear.setNameVi("");
        mockMvc.perform(put("/admin/categories/" + c.getId())
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clear)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nameVi").doesNotExist());
    }

    @Test
    void delete_categoryWithProduct_returns400() throws Exception {
        Brand brand = data.createBrand();
        Category category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        assertThat(product.getId()).isNotNull();

        mockMvc.perform(delete("/admin/categories/" + category.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("product")));
    }

    @Test
    void delete_categoryWithChildren_returns400() throws Exception {
        Category parent = categoryRepository.save(Category.builder()
                .slug("apparel").name("Apparel").sortOrder(0).isActive(true).build());
        categoryRepository.save(Category.builder()
                .slug("apparel-tees").name("Tees").parent(parent).sortOrder(0).isActive(true).build());

        mockMvc.perform(delete("/admin/categories/" + parent.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("sub-categories")));
    }

    @Test
    void delete_emptyCategory_succeeds() throws Exception {
        Category c = categoryRepository.save(Category.builder()
                .slug("accessories").name("Accessories").sortOrder(0).isActive(true).build());

        mockMvc.perform(delete("/admin/categories/" + c.getId())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        assertThat(categoryRepository.findById(c.getId())).isEmpty();
    }

    @Test
    void list_returnsTreeWithChildren() throws Exception {
        Category parent = categoryRepository.save(Category.builder()
                .slug("apparel").name("Apparel").sortOrder(0).isActive(true).build());
        categoryRepository.save(Category.builder()
                .slug("apparel-tees").name("Tees").parent(parent).sortOrder(0).isActive(true).build());

        mockMvc.perform(get("/admin/categories").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("apparel"))
                .andExpect(jsonPath("$.data[0].children[0].slug").value("apparel-tees"));
    }

    @Test
    void update_clearParent_setsParentToNull() throws Exception {
        Category parent = categoryRepository.save(Category.builder()
                .slug("p").name("P").sortOrder(0).isActive(true).build());
        Category child = categoryRepository.save(Category.builder()
                .slug("c").name("C").parent(parent).sortOrder(0).isActive(true).build());

        mockMvc.perform(put("/admin/categories/" + child.getId())
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clearParent\":true}"))
                .andExpect(status().isOk());

        Category reloaded = categoryRepository.findById(child.getId()).orElseThrow();
        assertThat(reloaded.getParent()).isNull();
    }

    @Test
    void update_cycleAttempt_returns400() throws Exception {
        Category a = categoryRepository.save(Category.builder()
                .slug("ax").name("A").sortOrder(0).isActive(true).build());
        Category b = categoryRepository.save(Category.builder()
                .slug("bx").name("B").parent(a).sortOrder(0).isActive(true).build());
        Category c = categoryRepository.save(Category.builder()
                .slug("cx").name("C").parent(b).sortOrder(0).isActive(true).build());

        String body = "{\"parentId\":" + c.getId() + "}";
        mockMvc.perform(put("/admin/categories/" + a.getId())
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cycle")));
    }

    @Test
    void list_surfacesOrphansEvenIfDbHasCycle() throws Exception {
        Category a = categoryRepository.save(Category.builder()
                .slug("ay").name("A").sortOrder(0).isActive(true).build());
        Category b = categoryRepository.save(Category.builder()
                .slug("by").name("B").sortOrder(0).isActive(true).build());
        a.setParent(b);
        b.setParent(a);
        categoryRepository.saveAll(java.util.List.of(a, b));

        mockMvc.perform(get("/admin/categories").header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
