package com.uniform.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FalClientConfig {

    @Bean
    public RestClient falRestClient(FalProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutSeconds() * 1000);
        factory.setReadTimeout(props.getReadTimeoutSeconds() * 1000);

        builder.baseUrl(props.getBaseUrl()).requestFactory(factory);
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            builder.defaultHeader("Authorization", "Key " + props.getApiKey());
        }
        return builder.build();
    }
}
