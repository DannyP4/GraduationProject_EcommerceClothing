package com.uniform.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GhnClientConfig {

    @Bean
    public RestClient ghnRestClient(GhnProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(props.getTimeoutSeconds() * 1000);

        builder.baseUrl(props.getBaseUrl()).requestFactory(factory);
        if (props.getToken() != null && !props.getToken().isBlank()) {
            builder.defaultHeader("Token", props.getToken());
        }
        if (props.getShopId() != null) {
            builder.defaultHeader("ShopId", String.valueOf(props.getShopId()));
        }
        return builder.build();
    }
}
