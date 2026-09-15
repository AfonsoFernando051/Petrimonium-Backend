package com.jf.PetApp.application.investment.cache;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory TTL cache for plain market quotes ({@code getQuote}), following the same
 * pattern as {@link AssetDetailsCache}. Exists because opening the Wallet dashboard calls
 * {@code /investments}, {@code /summary} and {@code /allocation} in quick succession, and Summary
 * and Allocation both delegate to {@code GetPortfolioHoldingsUseCaseImpl} — without this cache,
 * every one of those three requests independently re-fetched a quote per distinct ticker from the
 * external provider, tripling calls to a rate-limited, sometimes-slow HTTP API for data that
 * cannot have changed in the seconds between them.
 *
 * <p>The TTL is much shorter than {@link AssetDetailsCache}'s 5 minutes on purpose: this is a live
 * market price, not enriched metadata, and needs to feel fresh on the very next manual refresh —
 * not just fast on the accidental triple-call from one dashboard load.
 *
 * <p>Only successful, real (non-simulated) quotes are cached — see {@code
 * GetPortfolioHoldingsUseCaseImpl#fetchCurrentPrice}. Caching "no quote available" would risk
 * papering over a transient provider outage for the whole TTL instead of retrying on the very next
 * request, which is the safer default here.
 */
@Component
public class QuoteCache {

    private static final Logger log = LoggerFactory.getLogger(QuoteCache.class);

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(45);

    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();

    /**
     * Returns the cached quote if present and not expired, otherwise {@code null}.
     */
    public AssetQuoteResponse get(String ticker) {
        CachedEntry entry = cache.get(normalizeKey(ticker));
        if (entry == null) return null;
        if (entry.isExpired()) {
            cache.remove(normalizeKey(ticker));
            return null;
        }
        return entry.value;
    }

    /**
     * Stores a quote for the given ticker with the default TTL.
     */
    public void put(String ticker, AssetQuoteResponse value) {
        cache.put(normalizeKey(ticker), new CachedEntry(value, Instant.now().plus(DEFAULT_TTL)));
        log.debug("Cached quote for {} (expires in {})", ticker, DEFAULT_TTL);
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

    private record CachedEntry(AssetQuoteResponse value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
