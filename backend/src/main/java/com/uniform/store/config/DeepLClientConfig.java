package com.uniform.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DeepLClientConfig {

    @Bean
    public RestClient deeplRestClient(DeepLProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(props.getTimeoutSeconds() * 1000);

        builder.baseUrl(props.getBaseUrl()).requestFactory(factory);
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            builder.defaultHeader("Authorization", "DeepL-Auth-Key " + props.getApiKey());
        }
        return builder.build();
    }
}
