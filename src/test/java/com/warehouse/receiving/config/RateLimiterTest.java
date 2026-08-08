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
}
