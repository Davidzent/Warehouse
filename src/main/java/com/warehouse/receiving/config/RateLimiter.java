package com.warehouse.receiving.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory token bucket per (client, tier). Continuous refill, not a fixed
 * window, which would allow a double burst across the boundary. Single-instance
 * only: behind replicas each enforces the limit separately.
 */
public class RateLimiter {

    public enum Tier {
        TOKEN_MINT,
        WRITE,
        READ
    }

    private static final long NANOS_PER_MINUTE = 60_000_000_000L;

    private final RateLimitProperties properties;

    /** Access-ordered LRU. Synchronized because eviction observes the whole map. */
    private final Map<String, Bucket> buckets;

    /** Held outside the map on purpose: an evicted global bucket would come back full. */
    private final Bucket globalWrite;

    public RateLimiter(RateLimitProperties properties) {
        this.properties = properties;
        this.globalWrite = new Bucket(properties.getGlobalWritePerMinute());
        int cap = properties.getMaxTrackedClients();
        this.buckets = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                return size() > cap;
            }
        });
    }

    /**
     * Writes are metered twice: once against the caller, then against everyone.
     * A caller already over its own limit never reaches the shared budget, so a
     * flood cannot starve the global bucket faster than it is refused anyway.
     */
    public Decision check(String clientId, Tier tier) {
        int permits = permitsFor(tier);
        Bucket bucket = buckets.computeIfAbsent(clientId + '|' + tier, key -> new Bucket(permits));
        Decision perClient = bucket.tryConsume(System.nanoTime());

        if (!perClient.allowed() || tier != Tier.WRITE) {
            return perClient;
        }
        return globalWrite.tryConsume(System.nanoTime());
    }

    private int permitsFor(Tier tier) {
        return switch (tier) {
            case TOKEN_MINT -> properties.getTokenMintPerMinute();
            case WRITE -> properties.getWritePerMinute();
            case READ -> properties.getReadPerMinute();
        };
    }

    int trackedClients() {
        return buckets.size();
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {

        static Decision allow() {
            return new Decision(true, 0);
        }

        static Decision deny(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }
    }

    private static final class Bucket {

        private final double capacity;
        private final double nanosPerToken;

        private double tokens;
        private long lastRefillNanos;

        Bucket(int permitsPerMinute) {
            // Zero would divide by zero below; no config should mean "allow nothing".
            int safe = Math.max(1, permitsPerMinute);
            this.capacity = safe;
            this.nanosPerToken = (double) NANOS_PER_MINUTE / safe;
            this.tokens = safe;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized Decision tryConsume(long nowNanos) {
            refill(nowNanos);
            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                return Decision.allow();
            }
            // Round up: "retry in 0" invites a retry guaranteed to fail.
            long waitNanos = (long) Math.ceil((1.0d - tokens) * nanosPerToken);
            return Decision.deny(Math.max(1L, waitNanos / 1_000_000_000L + 1));
        }

        private void refill(long nowNanos) {
            long elapsed = Math.max(0L, nowNanos - lastRefillNanos);
            lastRefillNanos = nowNanos;
            tokens = Math.min(capacity, tokens + elapsed / nanosPerToken);
        }
    }
}
