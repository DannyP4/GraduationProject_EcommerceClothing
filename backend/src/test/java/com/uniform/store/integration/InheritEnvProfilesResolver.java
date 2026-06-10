package com.uniform.store.integration;

import org.springframework.test.context.ActiveProfilesResolver;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class InheritEnvProfilesResolver implements ActiveProfilesResolver {

    @Override
    public String[] resolve(Class<?> testClass) {
        String fromEnv = System.getProperty("spring.profiles.active",
                System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "docker"));
        Set<String> profiles = new LinkedHashSet<>(Arrays.asList(fromEnv.split("\\s*,\\s*")));
        profiles.add("test");
        return profiles.toArray(new String[0]);
    }
}
