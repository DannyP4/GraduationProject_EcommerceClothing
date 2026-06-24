package com.uniform.store.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
                .prefixCacheNameWith("vesta:");

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                CacheNames.CATEGORIES, defaultConfig.entryTtl(Duration.ofMinutes(30)),
                CacheNames.BRANDS, defaultConfig.entryTtl(Duration.ofMinutes(30)),
                CacheNames.BRAND_SUMMARIES, defaultConfig.entryTtl(Duration.ofMinutes(10)),
                CacheNames.PRODUCT_LISTS, defaultConfig.entryTtl(Duration.ofSeconds(90)),
                CacheNames.SIMILAR_PRODUCTS, defaultConfig.entryTtl(Duration.ofMinutes(15)),
                CacheNames.FREQUENTLY_BOUGHT_TOGETHER, defaultConfig.entryTtl(Duration.ofMinutes(15)),
                CacheNames.SIMILAR_TO_SET, defaultConfig.entryTtl(Duration.ofMinutes(15))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
