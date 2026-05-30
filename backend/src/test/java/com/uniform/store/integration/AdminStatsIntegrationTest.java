package com.uniform.store.integration;

import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminStatsIntegrationTest extends BaseIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    User customer1;
    User customer2;
    User customer3;
    User admin;
    String customerJwt;
    String adminJwt;
    LocalDate today;

    @BeforeEach
    void seed() {
        customer1 = data.createCustomer("c1@uniform.test", "Test1234");
        customer2 = data.createCustomer("c2@uniform.test", "Test1234");
        customer3 = data.createCustomer("c3@uniform.test", "Test1234");
        admin = data.createAdmin("statsadmin@uniform.test", "Admin1234");
        customerJwt = data.accessTokenFor(customer1.getEmail());
        adminJwt = data.accessTokenFor(admin.getEmail());

        Brand brand = data.createBrand();
        Category category = data.createCategory();

        Product p1 = data.createProduct(brand, category, new BigDecimal("200000"));
        Product p2 = data.createProduct(brand, category, new BigDecimal("500000"));
        ProductVariant v1 = data.createVariant(p1, 50);
        ProductVariant v2 = data.createVariant(p2, 50);

        today = LocalDate.now(ZONE);

        data.createOrderWithItem(customer1, v1, 2, OrderStatus.PAID,
                today.minusDays(5).atStartOfDay(ZONE).toInstant(), PaymentProvider.COD);
        data.createOrderWithItem(customer1, v1, 1, OrderStatus.DELIVERED,
                today.minusDays(10).atStartOfDay(ZONE).toInstant(), PaymentProvider.VNPAY);
        data.createOrderWithItem(customer2, v2, 3, OrderStatus.SHIPPED,
                today.minusDays(3).atStartOfDay(ZONE).toInstant(), PaymentProvider.STRIPE);
        data.createOrderWithItem(customer2, v2, 1, OrderStatus.CANCELLED,
                today.minusDays(2).atStartOfDay(ZONE).toInstant(), PaymentProvider.COD);
        data.createOrderWithItem(customer3, v1, 1, OrderStatus.PENDING,
                today.minusDays(7).atStartOfDay(ZONE).toInstant(), null);
    }

    @Test
    void summary_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/admin/stats/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void summary_withCustomerJwt_returns403() throws Exception {
        mockMvc.perform(get("/admin/stats/summary")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void summary_withAdminJwt_returnsRevenueAndOrders() throws Exception {
        mockMvc.perform(get("/admin/stats/summary")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current.revenue").value(2190000))
                .andExpect(jsonPath("$.data.current.orders").value(3))
                .andExpect(jsonPath("$.data.current.newCustomers").value(3))
                .andExpect(jsonPath("$.data.from").exists())
                .andExpect(jsonPath("$.data.to").exists())
                .andExpect(jsonPath("$.data.previous").exists());
    }

    @Test
    void summary_explicitRange_appliesFilter() throws Exception {
        mockMvc.perform(get("/admin/stats/summary")
                        .param("from", today.minusDays(6).toString())
                        .param("to", today.toString())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current.revenue").value(1960000))
                .andExpect(jsonPath("$.data.current.orders").value(2));
    }

    @Test
    void summary_fromAfterTo_returns400() throws Exception {
        mockMvc.perform(get("/admin/stats/summary")
                        .param("from", today.toString())
                        .param("to", today.minusDays(5).toString())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("on or before")));
    }

    @Test
    void revenue_byDay_returnsBucketsWithOrderCounts() throws Exception {
        mockMvc.perform(get("/admin/stats/revenue")
                        .param("granularity", "DAY")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].bucket").exists())
                .andExpect(jsonPath("$.data[0].revenue").exists())
                .andExpect(jsonPath("$.data[0].orderCount").exists());
    }

    @Test
    void revenue_byMonth_returnsSingleBucket() throws Exception {
        mockMvc.perform(get("/admin/stats/revenue")
                        .param("granularity", "MONTH")
                        .param("from", today.minusDays(10).toString())
                        .param("to", today.toString())
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void revenue_invalidGranularity_returns400() throws Exception {
        mockMvc.perform(get("/admin/stats/revenue")
                        .param("granularity", "HOUR")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paymentBreakdown_returnsProvidersSortedByRevenueDesc() throws Exception {
        mockMvc.perform(get("/admin/stats/payment-breakdown")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].provider").value("STRIPE"))
                .andExpect(jsonPath("$.data[0].revenue").value(1530000))
                .andExpect(jsonPath("$.data[1].provider").value("COD"))
                .andExpect(jsonPath("$.data[2].provider").value("VNPAY"))
                .andExpect(jsonPath("$.data[0].pct").exists());
    }

    @Test
    void paymentBreakdown_pctSumsTo100() throws Exception {
        mockMvc.perform(get("/admin/stats/payment-breakdown")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pct").value(org.hamcrest.Matchers.closeTo(69.86, 0.1)))
                .andExpect(jsonPath("$.data[1].pct").value(org.hamcrest.Matchers.closeTo(19.63, 0.1)))
                .andExpect(jsonPath("$.data[2].pct").value(org.hamcrest.Matchers.closeTo(10.50, 0.1)));
    }

    @Test
    void ordersByStatus_returnsAllSevenStatusesZeroFilled() throws Exception {
        mockMvc.perform(get("/admin/stats/orders-by-status")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(7))
                .andExpect(jsonPath("$.data[?(@.status=='PENDING')].count").value(1))
                .andExpect(jsonPath("$.data[?(@.status=='PAID')].count").value(1))
                .andExpect(jsonPath("$.data[?(@.status=='PROCESSING')].count").value(0))
                .andExpect(jsonPath("$.data[?(@.status=='SHIPPED')].count").value(1))
                .andExpect(jsonPath("$.data[?(@.status=='DELIVERED')].count").value(1))
                .andExpect(jsonPath("$.data[?(@.status=='CANCELLED')].count").value(1))
                .andExpect(jsonPath("$.data[?(@.status=='REFUNDED')].count").value(0));
    }

    @Test
    void topProducts_sortedByRevenueDesc() throws Exception {
        mockMvc.perform(get("/admin/stats/top-products")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].revenue").value(1500000))
                .andExpect(jsonPath("$.data[0].unitsSold").value(3))
                .andExpect(jsonPath("$.data[1].revenue").value(600000));
    }

    @Test
    void topProducts_respectsLimitParam() throws Exception {
        mockMvc.perform(get("/admin/stats/top-products")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void topCustomers_sortedBySpentDesc() throws Exception {
        mockMvc.perform(get("/admin/stats/top-customers")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].email").value("c2@uniform.test"))
                .andExpect(jsonPath("$.data[0].totalSpent").value(1530000))
                .andExpect(jsonPath("$.data[1].email").value("c1@uniform.test"))
                .andExpect(jsonPath("$.data[1].totalSpent").value(660000));
    }

    @Test
    void topCustomers_respectsLimitParam() throws Exception {
        mockMvc.perform(get("/admin/stats/top-customers")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void allEndpoints_rejectCustomerJwt() throws Exception {
        String[] paths = {
                "/admin/stats/summary",
                "/admin/stats/revenue",
                "/admin/stats/payment-breakdown",
                "/admin/stats/orders-by-status",
                "/admin/stats/top-products",
                "/admin/stats/top-customers"
        };
        for (String path : paths) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + customerJwt))
                    .andExpect(status().isForbidden());
        }
    }
}
