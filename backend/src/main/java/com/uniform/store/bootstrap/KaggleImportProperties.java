package com.uniform.store.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.kaggle-import")
public class KaggleImportProperties {

    private boolean enabled = false;
    private String path = "";
    private int limit = 500;
}
