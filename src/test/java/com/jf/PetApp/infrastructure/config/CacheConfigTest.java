package com.jf.PetApp.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheConfigTest {

    private final CacheConfig config = new CacheConfig();

    @Test
    void cacheManager_IsAConcurrentMapCacheManager() {
        CacheManager cacheManager = config.cacheManager();

        assertInstanceOf(ConcurrentMapCacheManager.class, cacheManager);
    }

    @Test
    void cacheManager_PreConfiguresTheAcademyCatalogCache() {
        CacheManager cacheManager = config.cacheManager();

        Cache cache = cacheManager.getCache(CacheConfig.ACADEMY_CATALOG_CACHE);

        assertNotNull(cache);
    }

    @Test
    void cacheManager_TheAcademyCatalogCacheIsUsableEndToEnd() {
        CacheManager cacheManager = config.cacheManager();
        Cache cache = cacheManager.getCache(CacheConfig.ACADEMY_CATALOG_CACHE);

        cache.put("en", "cached-catalog");

        assertEquals("cached-catalog", cache.get("en").get());
    }

    @Test
    void cacheManager_EachCallReturnsAFreshManagerWithAnEmptyCache() {
        // The @Bean method itself is stateless -- Spring's container is what makes it a
        // singleton at runtime -- so two direct calls must not share cache state.
        Cache firstManagerCache = config.cacheManager().getCache(CacheConfig.ACADEMY_CATALOG_CACHE);
        firstManagerCache.put("en", "cached-catalog");

        Cache secondManagerCache = config.cacheManager().getCache(CacheConfig.ACADEMY_CATALOG_CACHE);

        assertNotNull(secondManagerCache);
        assertEquals(null, secondManagerCache.get("en"));
    }
}
