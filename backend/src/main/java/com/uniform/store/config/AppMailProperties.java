package com.uniform.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class AppMailProperties {

    private boolean enabled = true;
    private String fromName = "Vesta";
    private String fromAddress;
    private String supportEmail;
}
