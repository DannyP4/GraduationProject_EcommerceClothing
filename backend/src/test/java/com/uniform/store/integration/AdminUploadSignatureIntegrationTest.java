package com.uniform.store.integration;

import com.uniform.store.dto.request.UploadSignatureRequest;
import com.uniform.store.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUploadSignatureIntegrationTest extends BaseIntegrationTest {

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
    void sign_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/admin/upload-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sign_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(post("/admin/upload-signature")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sign_withAdminJwt_returnsSignaturePayload() throws Exception {
        UploadSignatureRequest req = new UploadSignatureRequest();
        req.setFilenameHint("hero-tee");

        mockMvc.perform(post("/admin/upload-signature")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cloudName").exists())
                .andExpect(jsonPath("$.data.apiKey").exists())
                .andExpect(jsonPath("$.data.timestamp").isNumber())
                .andExpect(jsonPath("$.data.folder").value("uniform/products"))
                .andExpect(jsonPath("$.data.publicId").value(matchesPattern("hero-tee-[0-9a-f]{8}")))
                .andExpect(jsonPath("$.data.signature").value(matchesPattern("[0-9a-f]{40}")));
    }

    @Test
    void sign_emptyBody_returnsSignatureWithDefaults() throws Exception {
        mockMvc.perform(post("/admin/upload-signature")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.folder").value("uniform/products"))
                .andExpect(jsonPath("$.data.publicId").value(matchesPattern("upload-[0-9a-f]{8}")));
    }

    @Test
    void sign_customFolder_isReflectedInResponse() throws Exception {
        UploadSignatureRequest req = new UploadSignatureRequest();
        req.setFolder("uniform/heroes");

        mockMvc.perform(post("/admin/upload-signature")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.folder").value("uniform/heroes"));
    }

    @Test
    void sign_invalidFolderCharacters_returns400() throws Exception {
        UploadSignatureRequest req = new UploadSignatureRequest();
        req.setFolder("uniform/../etc");

        mockMvc.perform(post("/admin/upload-signature")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
