package com.uniform.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.gemini")
@Getter
@Setter
public class GeminiProperties {

    private String apiKey;
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String embeddingModel = "gemini-embedding-001";
    private String chatModel = "gemini-2.5-flash";
    private int embeddingDim = 3072;
    private int batchSize = 100;
    private int timeoutSeconds = 30;

    // chat generation
    private double temperature = 0.3;
    private int maxOutputTokens = 1024;
    private int thinkingBudget = 0;
    private int retrievalTopK = 6;
    private double scoreThreshold = 0.63;
    private int historyMaxTurns = 3;
    private int trendingFallbackSize = 6;
}
