package com.uniform.store.integration;

import com.uniform.store.dto.response.DistrictDto;
import com.uniform.store.dto.response.ProvinceDto;
import com.uniform.store.dto.response.WardDto;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.repository.AddressRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.service.impl.GhnClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GhnShippingIntegrationTest extends BaseIntegrationTest {

    @MockBean GhnClient ghnClient;
    @Autowired AddressRepository addressRepository;
    @Autowired OrderRepository orderRepository;

    User buyer;
    String buyerJwt;
    ProductVariant variant;

    @BeforeEach
    void seed() {
        buyer = data.createCustomer("buyer@uniform.test", "Test1234");
        buyerJwt = data.accessTokenFor(buyer.getEmail());
        Brand brand = data.createBrand();
        Category category = data.createCategory();
        Product product = data.createProduct(brand, category, new BigDecimal("250000"));
        variant = data.createVariant(product, 10);
    }

    @Test
    void provinces_returnsMappedGhnData() throws Exception {
        when(ghnClient.provinces()).thenReturn(List.of(new ProvinceDto(202, "Hồ Chí Minh")));

        mockMvc.perform(get("/shipping/provinces").header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(202))
                .andExpect(jsonPath("$.data[0].name").value("Hồ Chí Minh"));
    }

    @Test
    void districtsAndWards_returnMappedGhnData() throws Exception {
        when(ghnClient.districts(202)).thenReturn(List.of(new DistrictDto(3695, 202, "Thành Phố Thủ Đức")));
        when(ghnClient.wards(3695)).thenReturn(List.of(new WardDto("90737", 3695, "Phường Linh Trung")));

        mockMvc.perform(get("/shipping/districts").param("provinceId", "202")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3695));

        mockMvc.perform(get("/shipping/wards").param("districtId", "3695")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("90737"));
    }

    @Test
    void ghnQuote_returnsGhnFee() throws Exception {
        when(ghnClient.calculateFee(eq(3695), eq("90737"), anyInt())).thenReturn(Optional.of(new BigDecimal("40000")));

        mockMvc.perform(get("/shipping/ghn-quote")
                        .param("toDistrictId", "3695").param("toWardCode", "90737")
                        .param("quantity", "1").param("subtotal", "250000")
                        .header("Authorization", "Bearer " + buyerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee").value(40000));
    }

    @Test
    void placeOrder_withGhnAddress_usesGhnFee() throws Exception {
        when(ghnClient.calculateFee(anyInt(), anyString(), anyInt())).thenReturn(Optional.of(new BigDecimal("40000")));

        Address addr = addressRepository.save(Address.builder()
                .user(buyer).recipient("Buyer").phone("0901234567")
                .line1("1 Test St").district("Thủ Đức").city("HCM").country("VN")
                .ghnDistrictId(3695).ghnWardCode("90737")
                .isDefault(true).build());

        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "variantId", variant.getId(), "quantity", 1,
                "addressId", addr.getId(), "paymentMethod", "COD"));

        String resp = mockMvc.perform(post("/orders/direct")
                        .header("Authorization", "Bearer " + buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(resp).path("data").path("order").path("orderNumber").asText();
        assertThat(orderRepository.findByOrderNumber(orderNumber).orElseThrow().getShippingCost())
                .isEqualByComparingTo("40000");
    }
}
