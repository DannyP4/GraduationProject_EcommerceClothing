package com.uniform.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.shipping.ghn")
@Getter
@Setter
public class GhnProperties {

    private boolean enabled = true;
    private String baseUrl = "https://dev-online-gateway.ghn.vn";
    private String token;
    private Integer shopId;
    private int timeoutSeconds = 15;

    // Shop pickup origin.
    private Integer fromDistrictId;
    private String fromWardCode;

    private int serviceTypeId = 2;
    private int defaultItemWeightGrams = 500;
}
