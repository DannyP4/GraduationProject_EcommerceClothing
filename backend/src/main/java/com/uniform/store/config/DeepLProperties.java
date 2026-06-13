package com.uniform.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.translation.deepl")
@Getter
@Setter
public class DeepLProperties {

    private boolean enabled = true;
    private String apiKey;
    private String baseUrl = "https://api-free.deepl.com";
    private int timeoutSeconds = 30;
    private int batchSize = 40;
}
