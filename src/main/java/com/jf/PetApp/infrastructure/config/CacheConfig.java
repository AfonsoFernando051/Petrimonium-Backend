package com.jf.PetApp.infrastructure.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Academy catalog is seeded once at boot ({@code AcademyContentSeedRunner})
 * and never written to afterward, so a plain in-memory cache keyed by
 * language is safe for the lifetime of the process — no eviction needed.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ACADEMY_CATALOG_CACHE = "academyCatalog";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(ACADEMY_CATALOG_CACHE);
    }
}
