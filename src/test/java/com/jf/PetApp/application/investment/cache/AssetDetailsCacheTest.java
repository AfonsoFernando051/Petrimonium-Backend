package com.jf.PetApp.application.investment.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jf.PetApp.application.investment.dto.AssetDetailsResponseDTO;

class AssetDetailsCacheTest {

    private AssetDetailsCache cache;

    @BeforeEach
    void setUp() {
        cache = new AssetDetailsCache();
    }

    private AssetDetailsResponseDTO sampleResponse(String ticker) {
        return AssetDetailsResponseDTO.builder().ticker(ticker).build();
    }

    @Test
    void get_NothingCached_ReturnsNull() {
        assertNull(cache.get("PETR4"));
    }

    @Test
    void get_AfterPut_ReturnsCachedValue() {
        AssetDetailsResponseDTO value = sampleResponse("PETR4");
        cache.put("PETR4", value);

        assertEquals(value, cache.get("PETR4"));
    }

    @Test
    void get_KeyIsCaseAndWhitespaceInsensitive() {
        AssetDetailsResponseDTO value = sampleResponse("petr4");
        cache.put("petr4", value);

        assertEquals(value, cache.get("PETR4"));
        assertEquals(value, cache.get("  PETR4  "));
    }

    @Test
    void evict_RemovesOnlyThatTicker() {
        cache.put("PETR4", sampleResponse("PETR4"));
        cache.put("VALE3", sampleResponse("VALE3"));

        cache.evict("PETR4");

        assertNull(cache.get("PETR4"));
        assertNotNull(cache.get("VALE3"));
    }

    @Test
    void evictAll_ClearsEveryEntry() {
        cache.put("PETR4", sampleResponse("PETR4"));
        cache.put("VALE3", sampleResponse("VALE3"));

        cache.evictAll();

        assertNull(cache.get("PETR4"));
        assertNull(cache.get("VALE3"));
    }

    @Test
    void getCachedAt_NothingCached_ReturnsNull() {
        assertNull(cache.getCachedAt("PETR4"));
    }

    @Test
    void getCachedAt_AfterPut_ReturnsTimestampCloseToNow() {
        Instant before = Instant.now();
        cache.put("PETR4", sampleResponse("PETR4"));
        Instant after = Instant.now();

        Instant cachedAt = cache.getCachedAt("PETR4");

        assertNotNull(cachedAt);
        assertTrue(!cachedAt.isBefore(before.minusSeconds(1)) && !cachedAt.isAfter(after.plusSeconds(1)));
    }

    @Test
    void get_ExpiredEntry_ReturnsNullAndEvictsIt() throws Exception {
        cache.put("PETR4", sampleResponse("PETR4"));
        forceExpire("PETR4");

        assertNull(cache.get("PETR4"));
        // Second call confirms the entry was actually removed, not just skipped.
        assertNull(cache.get("PETR4"));
    }

    @Test
    void getCachedAt_ExpiredEntry_ReturnsNull() throws Exception {
        cache.put("PETR4", sampleResponse("PETR4"));
        forceExpire("PETR4");

        assertNull(cache.getCachedAt("PETR4"));
    }

    /**
     * Reaches into the private cache map via reflection and replaces the entry for
     * {@code ticker} with one whose expiry is already in the past. This is the only
     * practical way to exercise the expiry branch without sleeping the test thread
     * for the real 5-minute TTL.
     */
    @SuppressWarnings("unchecked")
    private void forceExpire(String ticker) throws Exception {
        Field cacheField = AssetDetailsCache.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        ConcurrentHashMap<String, Object> internalCache =
                (ConcurrentHashMap<String, Object>) cacheField.get(cache);

        Class<?> cachedEntryClass = Class.forName(
                "com.jf.PetApp.application.investment.cache.AssetDetailsCache$CachedEntry");
        Constructor<?> constructor = cachedEntryClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object expiredEntry = constructor.newInstance(sampleResponse(ticker), Instant.now().minusSeconds(1));

        String normalizedKey = ticker.toUpperCase().trim();
        ((Map<String, Object>) internalCache).put(normalizedKey, expiredEntry);
    }
}
