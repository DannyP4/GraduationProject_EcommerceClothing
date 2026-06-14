package com.uniform.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.tryon.fal")
@Getter
@Setter
public class FalProperties {

    private boolean enabled = true;
    private String apiKey;
    private String baseUrl = "https://queue.fal.run";
    private String model = "fal-ai/fashn/tryon/v1.6";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 60;
}
