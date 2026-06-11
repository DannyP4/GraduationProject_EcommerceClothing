package com.uniform.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
@Getter
@Setter
public class AppAuthProperties {

    private int resetTokenTtlMinutes = 60;
    private int verifyTokenTtlHours = 48;
    private int adminInviteTtlHours = 24;
    private int oauthHandoffTtlSeconds = 120;
}
