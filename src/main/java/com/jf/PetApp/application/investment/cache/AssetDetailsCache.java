package com.jf.PetApp.application.investment.cache;

import com.jf.PetApp.application.investment.dto.AssetDetailsResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory TTL cache for enriched asset details. Avoids hitting
 * the external provider on every request for the same ticker (the same user
 * opening the same asset page multiple times, or multiple users viewing
 * the same ticker within a short window).
 *
 * <p>Market-sensitive data (price, change%) uses a short TTL; metadata
 * (sector, description, logo) could use a longer one, but for simplicity
 * the entire response shares a single TTL per ticker. A future Redis or
 * Caffeine migration can replace this class without touching callers.</p>
 */
@Component
public class AssetDetailsCache {

    private static final Logger log = LoggerFactory.getLogger(AssetDetailsCache.class);

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();

    /**
     * Returns the cached response if present and not expired, otherwise {@code null}.
     */
    public AssetDetailsResponseDTO get(String ticker) {
        CachedEntry entry = cache.get(normalizeKey(ticker));
        if (entry == null) return null;
        if (entry.isExpired()) {
            cache.remove(normalizeKey(ticker));
            return null;
        }
        return entry.value;
    }

    /**
     * Stores a response for the given ticker with the default TTL.
     */
    public void put(String ticker, AssetDetailsResponseDTO value) {
        cache.put(normalizeKey(ticker), new CachedEntry(value, Instant.now().plus(DEFAULT_TTL)));
        log.debug("Cached asset details for {} (expires in {})", ticker, DEFAULT_TTL);
    }

    /**
     * Returns the timestamp when the cached entry was stored, or {@code null}
     * if the ticker is not cached.
     */
    public Instant getCachedAt(String ticker) {
        CachedEntry entry = cache.get(normalizeKey(ticker));
        if (entry == null || entry.isExpired()) return null;
        return entry.expiresAt.minus(DEFAULT_TTL);
    }

    public void evict(String ticker) {
        cache.remove(normalizeKey(ticker));
    }

    public void evictAll() {
        cache.clear();
    }

    private String normalizeKey(String ticker) {
        return ticker.toUpperCase().trim();
    }

    private record CachedEntry(AssetDetailsResponseDTO value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
