package com.molla.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.List;

/**
 * In-memory cache configuration. Used when Redis is excluded (default).
 * Caches are created on demand; these names match @Cacheable / @CacheEvict usage.
 * Not used when Redis is enabled (RedisAutoConfiguration provides RedisCacheManager).
 */
@Configuration
public class CacheConfig {

    public static final List<String> CACHE_NAMES = List.of(
            "users", "usersAll",
            "stores", "storesAll", "storesByAdmin",
            "branches", "branchesByStore",
            "productsByStore", "productsAll",
            "categoriesByStore",
            "inventory", "inventoriesByBranch",
            "orders", "recentOrdersByBranch"
    );

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        manager.setCacheNames(CACHE_NAMES);
        return manager;
    }
}
