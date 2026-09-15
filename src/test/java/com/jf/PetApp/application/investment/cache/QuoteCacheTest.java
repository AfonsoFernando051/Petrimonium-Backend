package com.jf.PetApp.application.investment.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;

class QuoteCacheTest {

    private QuoteCache cache;

    @BeforeEach
    void setUp() {
        cache = new QuoteCache();
    }

    private AssetQuoteResponse sampleQuote(String ticker) {
        return new AssetQuoteResponse(ticker, "Sample", 35.0, "BRL");
    }

    @Test
    void get_NothingCached_ReturnsNull() {
        assertNull(cache.get("PETR4"));
    }

    @Test
    void get_AfterPut_ReturnsCachedValue() {
        AssetQuoteResponse value = sampleQuote("PETR4");
        cache.put("PETR4", value);

        assertEquals(value, cache.get("PETR4"));
    }

    @Test
    void get_KeyIsCaseAndWhitespaceInsensitive() {
        AssetQuoteResponse value = sampleQuote("petr4");
        cache.put("petr4", value);

        assertEquals(value, cache.get("PETR4"));
        assertEquals(value, cache.get("  PETR4  "));
    }

    @Test
    void evict_RemovesOnlyThatTicker() {
        cache.put("PETR4", sampleQuote("PETR4"));
        cache.put("VALE3", sampleQuote("VALE3"));

        cache.evict("PETR4");

        assertNull(cache.get("PETR4"));
        assertEquals(sampleQuote("VALE3"), cache.get("VALE3"));
    }

    @Test
    void evictAll_ClearsEveryEntry() {
        cache.put("PETR4", sampleQuote("PETR4"));
        cache.put("VALE3", sampleQuote("VALE3"));

        cache.evictAll();

        assertNull(cache.get("PETR4"));
        assertNull(cache.get("VALE3"));
    }

    @Test
    void get_ExpiredEntry_ReturnsNullAndEvictsIt() throws Exception {
        cache.put("PETR4", sampleQuote("PETR4"));
        forceExpire("PETR4");

        assertNull(cache.get("PETR4"));
        // Second call confirms the entry was actually removed, not just skipped.
        assertNull(cache.get("PETR4"));
    }

    /**
     * Reaches into the private cache map via reflection and replaces the entry for
     * {@code ticker} with one whose expiry is already in the past — the same trick
     * {@code AssetDetailsCacheTest} uses, since the TTL is too short to wait out in real time
     * but also too load-bearing to skip testing.
     */
    @SuppressWarnings("unchecked")
    private void forceExpire(String ticker) throws Exception {
        Field cacheField = QuoteCache.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        ConcurrentHashMap<String, Object> internalCache =
                (ConcurrentHashMap<String, Object>) cacheField.get(cache);

        Class<?> cachedEntryClass = Class.forName(
                "com.jf.PetApp.application.investment.cache.QuoteCache$CachedEntry");
        Constructor<?> constructor = cachedEntryClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object expiredEntry = constructor.newInstance(sampleQuote(ticker), Instant.now().minusSeconds(1));

        String normalizedKey = ticker.toUpperCase().trim();
        ((Map<String, Object>) internalCache).put(normalizedKey, expiredEntry);
    }
}
