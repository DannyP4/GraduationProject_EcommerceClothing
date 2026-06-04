package com.uniform.store.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.catalog-seed")
public class CatalogSeedProperties {

    private boolean enabled = true;
}
