package com.paymentprocessor.authorization.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Permission caching. Resolved effective-permission sets and role-permission mappings are cached
 * with a short TTL to keep authorization latency low, and are evicted explicitly on role/permission
 * changes (see {@code PermissionCacheService}).
 */
@Configuration
public class CacheConfig {

    public static final String EFFECTIVE_PERMISSIONS = "effectivePermissions";
    public static final String ROLE_PERMISSIONS = "rolePermissions";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                EFFECTIVE_PERMISSIONS, ROLE_PERMISSIONS);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(100_000)
                .recordStats());
        return manager;
    }
}
