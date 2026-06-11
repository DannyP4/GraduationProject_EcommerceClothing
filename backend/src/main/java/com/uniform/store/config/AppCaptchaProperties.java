package com.uniform.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.captcha")
@Getter
@Setter
public class AppCaptchaProperties {

    private boolean enabled = true;
    private Turnstile turnstile = new Turnstile();

    @Getter
    @Setter
    public static class Turnstile {
        private String secretKey;
        private String verifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    }
}
