package com.jf.PetApp.infrastructure.security.ratelimit;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * A single trusted-proxy CIDR range (IPv4 only — see {@link RateLimitingFilter}'s doc). Used
 * to decide whether X-Forwarded-For, set by whatever sent this request, can be trusted: only
 * when the actual TCP peer address is itself inside one of the configured ranges.
 */
public final class TrustedProxyMatcher {

    private final int networkBits;
    private final int prefixLength;

    private TrustedProxyMatcher(int networkBits, int prefixLength) {
        this.networkBits = networkBits;
        this.prefixLength = prefixLength;
    }

    /**
     * Parses a single "a.b.c.d/nn" CIDR range. Returns empty (rather than throwing) for
     * anything malformed or non-IPv4 — a bad entry in configuration should be silently
     * ignored (trusting nothing) rather than crashing the whole rate limiter, and definitely
     * never silently trusted.
     */
    public static Optional<TrustedProxyMatcher> parse(String cidr) {
        String[] parts = cidr.split("/", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }
        int prefixLength;
        try {
            prefixLength = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (prefixLength < 0 || prefixLength > 32) {
            return Optional.empty();
        }

        Integer address = toIpv4Int(parts[0].trim());
        if (address == null) {
            return Optional.empty();
        }

        int mask = prefixLength == 0 ? 0 : (-1 << (32 - prefixLength));
        return Optional.of(new TrustedProxyMatcher(address & mask, prefixLength));
    }

    public boolean matches(String candidateAddress) {
        Integer candidate = toIpv4Int(candidateAddress);
        if (candidate == null) {
            return false;
        }
        int mask = prefixLength == 0 ? 0 : (-1 << (32 - prefixLength));
        return (candidate & mask) == networkBits;
    }

    private static Integer toIpv4Int(String address) {
        try {
            InetAddress parsed = InetAddress.getByName(address);
            byte[] bytes = parsed.getAddress();
            if (bytes.length != 4) {
                // IPv6 or otherwise unsupported — never matches, never trusted.
                return null;
            }
            return ((bytes[0] & 0xFF) << 24)
                    | ((bytes[1] & 0xFF) << 16)
                    | ((bytes[2] & 0xFF) << 8)
                    | (bytes[3] & 0xFF);
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
