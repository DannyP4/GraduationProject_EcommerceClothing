package com.uniform.store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniform.store.config.GhnProperties;
import com.uniform.store.dto.response.DistrictDto;
import com.uniform.store.dto.response.ProvinceDto;
import com.uniform.store.dto.response.WardDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GhnClient {

    private static final String MASTER = "/shiip/public-api/master-data";
    private static final String FEE = "/shiip/public-api/v2/shipping-order/fee";
    private static final String CREATE = "/shiip/public-api/v2/shipping-order/create";
    private static final String DETAIL = "/shiip/public-api/v2/shipping-order/detail";
    private static final Collator VI_COLLATOR = Collator.getInstance(Locale.forLanguageTag("vi"));

    // Seller bears the GHN fee
    private static final int PAYMENT_TYPE_SELLER = 1;
    private static final String REQUIRED_NOTE = "KHONGCHOXEMHANG";

    private final RestClient ghnRestClient;
    private final GhnProperties props;

    // GHN seeds junk entries
    static boolean isMockProvinceName(String name) {
        if (name == null || name.isBlank()) return true;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("test") || lower.contains("alert") || name.matches(".*\\s\\d+$");
    }

    public static boolean isDeliveredStatus(String status) {
        return "delivered".equalsIgnoreCase(status);
    }

    public boolean isEnabled() {
        return props.isEnabled() && props.getToken() != null && !props.getToken().isBlank();
    }

    public List<ProvinceDto> provinces() {
        JsonNode data = fetch(MASTER + "/province");
        List<ProvinceDto> out = new ArrayList<>();
        if (data != null) {
            for (JsonNode n : data) {
                String name = n.path("ProvinceName").asText();
                if (isMockProvinceName(name)) continue;
                out.add(new ProvinceDto(n.path("ProvinceID").asInt(), name));
            }
        }
        out.sort(Comparator.comparing(ProvinceDto::getName, VI_COLLATOR));
        return out;
    }

    public List<DistrictDto> districts(int provinceId) {
        JsonNode data = fetch(MASTER + "/district?province_id=" + provinceId);
        List<DistrictDto> out = new ArrayList<>();
        if (data != null) {
            for (JsonNode n : data) {
                out.add(new DistrictDto(n.path("DistrictID").asInt(), n.path("ProvinceID").asInt(),
                        n.path("DistrictName").asText()));
            }
        }
        out.sort(Comparator.comparing(DistrictDto::getName, VI_COLLATOR));
        return out;
    }

    public List<WardDto> wards(int districtId) {
        JsonNode data = fetch(MASTER + "/ward?district_id=" + districtId);
        List<WardDto> out = new ArrayList<>();
        if (data != null) {
            for (JsonNode n : data) {
                out.add(new WardDto(n.path("WardCode").asText(), n.path("DistrictID").asInt(),
                        n.path("WardName").asText()));
            }
        }
        out.sort(Comparator.comparing(WardDto::getName, VI_COLLATOR));
        return out;
    }

    public Optional<BigDecimal> calculateFee(int toDistrictId, String toWardCode, int weightGrams) {
        if (!isEnabled() || props.getFromDistrictId() == null) {
            return Optional.empty();
        }
        Map<String, Object> body = Map.of(
                "from_district_id", props.getFromDistrictId(),
                "service_type_id", props.getServiceTypeId(),
                "to_district_id", toDistrictId,
                "to_ward_code", toWardCode,
                "weight", Math.max(weightGrams, 1),
                "length", 20, "width", 20, "height", 10,
                "insurance_value", 0);
        try {
            JsonNode resp = ghnRestClient.post().uri(FEE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp != null && resp.path("code").asInt() == 200 && resp.path("data").has("total")) {
                return Optional.of(BigDecimal.valueOf(resp.path("data").path("total").asLong()));
            }
            log.warn("GHN fee unavailable: {}", resp != null ? resp.path("message").asText() : "null response");
        } catch (RestClientException e) {
            log.warn("GHN fee call failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> createOrder(CreateOrderCommand cmd) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        int unitWeight = Math.max(props.getDefaultItemWeightGrams(), 1);
        int totalWeight = cmd.items().stream().mapToInt(GhnItem::quantity).sum() * unitWeight;
        List<Map<String, Object>> items = cmd.items().stream()
                .map(i -> Map.<String, Object>of(
                        "name", i.name(),
                        "quantity", i.quantity(),
                        "weight", unitWeight))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payment_type_id", PAYMENT_TYPE_SELLER);
        body.put("required_note", REQUIRED_NOTE);
        body.put("to_name", cmd.toName());
        body.put("to_phone", cmd.toPhone());
        body.put("to_address", cmd.toAddress());
        body.put("to_ward_code", cmd.toWardCode());
        body.put("to_district_id", cmd.toDistrictId());
        body.put("cod_amount", cmd.codAmount());
        body.put("content", cmd.content());
        body.put("weight", Math.max(totalWeight, 1));
        body.put("length", 20);
        body.put("width", 20);
        body.put("height", 10);
        body.put("service_type_id", props.getServiceTypeId());
        body.put("items", items);

        try {
            JsonNode resp = ghnRestClient.post().uri(CREATE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp != null && resp.path("code").asInt() == 200) {
                String code = resp.path("data").path("order_code").asText(null);
                if (code != null && !code.isBlank()) {
                    return Optional.of(code);
                }
            }
            log.warn("GHN create-order unavailable: {}", resp != null ? resp.path("message").asText() : "null response");
        } catch (RestClientException e) {
            log.warn("GHN create-order call failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> getOrderStatus(String orderCode) {
        if (!isEnabled() || orderCode == null || orderCode.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode resp = ghnRestClient.post().uri(DETAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("order_code", orderCode))
                    .retrieve()
                    .body(JsonNode.class);
            if (resp != null && resp.path("code").asInt() == 200) {
                String status = resp.path("data").path("status").asText(null);
                if (status != null && !status.isBlank()) {
                    return Optional.of(status);
                }
            }
            log.warn("GHN detail unavailable for {}: {}", orderCode,
                    resp != null ? resp.path("message").asText() : "null response");
        } catch (RestClientException e) {
            log.warn("GHN detail call failed for {}: {}", orderCode, e.getMessage());
        }
        return Optional.empty();
    }

    public record GhnItem(String name, int quantity) {}

    public record CreateOrderCommand(int toDistrictId, String toWardCode, String toName, String toPhone,
                                     String toAddress, long codAmount, String content, List<GhnItem> items) {}

    private JsonNode fetch(String uri) {
        try {
            JsonNode resp = ghnRestClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (resp != null && resp.path("code").asInt() == 200) {
                return resp.path("data");
            }
            log.warn("GHN master-data {} returned {}", uri, resp != null ? resp.path("message").asText() : "null");
        } catch (RestClientException e) {
            log.warn("GHN master-data {} failed: {}", uri, e.getMessage());
        }
        return null;
    }
}
