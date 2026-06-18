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
    private static final Collator VI_COLLATOR = Collator.getInstance(Locale.forLanguageTag("vi"));

    private final RestClient ghnRestClient;
    private final GhnProperties props;

    // GHN seeds junk entries
    static boolean isMockProvinceName(String name) {
        if (name == null || name.isBlank()) return true;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("test") || lower.contains("alert") || name.matches(".*\\s\\d+$");
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
