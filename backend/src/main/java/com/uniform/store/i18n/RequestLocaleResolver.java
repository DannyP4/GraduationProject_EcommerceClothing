package com.uniform.store.i18n;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RequestLocaleResolver {

    private final Set<String> supported;
    private final String defaultLocale;

    public RequestLocaleResolver(
            @Value("${app.i18n.supported-locales:en,vi,ja}") String supportedRaw,
            @Value("${app.i18n.default-locale:en}") String defaultLocale) {
        this.supported = Arrays.stream(supportedRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
        this.defaultLocale = defaultLocale.toLowerCase();
    }

    @PostConstruct
    void logConfig() {
        log.info("RequestLocaleResolver initialized: supported={}, default={}", supported, defaultLocale);
    }

    public String resolve(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return defaultLocale;
        }
        String first = acceptLanguage.split(",")[0].trim();
        String primary = first.split(";")[0].trim();
        int dash = primary.indexOf('-');
        String tag = (dash > 0 ? primary.substring(0, dash) : primary).toLowerCase();
        return supported.contains(tag) ? tag : defaultLocale;
    }
}
