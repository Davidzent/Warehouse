package com.warehouse.receiving.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.warehouse.receiving.config.RateLimiter.Tier;

class RateLimiterTest {

    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setTokenMintPerMinute(3);
        properties.setWritePerMinute(5);
        properties.setReadPerMinute(10);
        // High enough that the per-client tests above are never the global cap.
        properties.setGlobalWritePerMinute(1000);
    }

    private RateLimiter limiter() {
        return new RateLimiter(properties);
    }

    private static long allowedOutOf(RateLimiter limiter, String client, Tier tier, int attempts) {
        long allowed = 0;
        for (int i = 0; i < attempts; i++) {
            if (limiter.check(client, tier).allowed()) {
                allowed++;
            }
        }
        return allowed;
    }

    @Test
    void allowsExactlyTheConfiguredBurstThenRefuses() {
        RateLimiter limiter = limiter();

        assertThat(allowedOutOf(limiter, "1.1.1.1", Tier.TOKEN_MINT, 10)).isEqualTo(3);
    }

    @Test
    void refusalCarriesAPositiveRetryAfter() {
        RateLimiter limiter = limiter();
        allowedOutOf(limiter, "1.1.1.1", Tier.TOKEN_MINT, 3);

        RateLimiter.Decision denied = limiter.check("1.1.1.1", Tier.TOKEN_MINT);

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isPositive();
    }

    @Test
    void clientsAreLimitedIndependently() {
        RateLimiter limiter = limiter();
        allowedOutOf(limiter, "1.1.1.1", Tier.TOKEN_MINT, 3);

        assertThat(limiter.check("2.2.2.2", Tier.TOKEN_MINT).allowed()).isTrue();
    }

    @Test
    void tiersDoNotShareABudget() {
        RateLimiter limiter = limiter();
        allowedOutOf(limiter, "1.1.1.1", Tier.TOKEN_MINT, 3);

        assertThat(limiter.check("1.1.1.1", Tier.READ).allowed()).isTrue();
        assertThat(limiter.check("1.1.1.1", Tier.WRITE).allowed()).isTrue();
    }

    @Test
    void eachTierUsesItsOwnConfiguredRate() {
        assertThat(allowedOutOf(limiter(), "1.1.1.1", Tier.WRITE, 20)).isEqualTo(5);
        assertThat(allowedOutOf(limiter(), "1.1.1.1", Tier.READ, 20)).isEqualTo(10);
    }

    @Test
    void trackedBucketsAreCappedSoSpoofedAddressesCannotExhaustMemory() {
        properties.setMaxTrackedClients(50);
        RateLimiter limiter = limiter();

        for (int i = 0; i < 500; i++) {
            limiter.check("10.0.0." + i, Tier.READ);
        }

        assertThat(limiter.trackedClients()).isLessThanOrEqualTo(50);
    }

    @Test
    void aRateOfZeroStillAllowsOneRatherThanDividingByZero() {
        properties.setWritePerMinute(0);

        assertThat(allowedOutOf(limiter(), "1.1.1.1", Tier.WRITE, 5)).isEqualTo(1);
    }

    /**
     * The per-IP key comes from a client-controlled header, so this is the only
     * limit a caller rotating addresses cannot walk around.
     */
    @Test
    void globalCapBoundsWritesAcrossEveryClient() {
        properties.setGlobalWritePerMinute(8);
        RateLimiter limiter = limiter();

        long allowed = 0;
        for (int i = 0; i < 40; i++) {
            if (limiter.check("10.0.0." + i, Tier.WRITE).allowed()) {
                allowed++;
            }
        }

        assertThat(allowed).isEqualTo(8);
    }

    @Test
    void globalCapDoesNotApplyToReadsOrTokenMinting() {
        properties.setGlobalWritePerMinute(1);
        RateLimiter limiter = limiter();
        limiter.check("1.1.1.1", Tier.WRITE);

        assertThat(limiter.check("2.2.2.2", Tier.WRITE).allowed()).isFalse();
        assertThat(limiter.check("2.2.2.2", Tier.READ).allowed()).isTrue();
        assertThat(limiter.check("2.2.2.2", Tier.TOKEN_MINT).allowed()).isTrue();
    }

    /** A caller over its own limit must not spend from the shared budget. */
    @Test
    void aClientRefusedByItsOwnLimitLeavesTheGlobalBudgetIntact() {
        properties.setWritePerMinute(2);
        properties.setGlobalWritePerMinute(5);
        RateLimiter limiter = limiter();

        allowedOutOf(limiter, "1.1.1.1", Tier.WRITE, 20); // 2 allowed, 18 refused

        // Two more clients, each capped at 2 of their own, so only the 3 shared
        // permits left decide the total — not 4.
        long remaining = allowedOutOf(limiter, "2.2.2.2", Tier.WRITE, 10)
                + allowedOutOf(limiter, "3.3.3.3", Tier.WRITE, 10);

        assertThat(remaining).isEqualTo(3);
    }

    /**
     * The global bucket is held outside the LRU. Were it a map entry, spoofed
     * addresses could evict it and it would come back full — a clean bypass.
     */
    @Test
    void globalCapSurvivesEvictionPressureFromSpoofedAddresses() {
        properties.setGlobalWritePerMinute(4);
        properties.setMaxTrackedClients(10);
        RateLimiter limiter = limiter();

        long allowed = 0;
        for (int i = 0; i < 500; i++) {
            if (limiter.check("10.1." + (i / 250) + "." + (i % 250), Tier.WRITE).allowed()) {
                allowed++;
            }
        }

        assertThat(allowed).isEqualTo(4);
    }
}
