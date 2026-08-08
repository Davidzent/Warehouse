package com.warehouse.receiving.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Per-IP request limits, per tier, per minute. */
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /** POST /api/auth/** — unauthenticated and grants a role. */
    private int tokenMintPerMinute = 5;

    /** POST /api/receipts — writes stock that cannot be undone. */
    private int writePerMinute = 10;

    private int readPerMinute = 60;

    /**
     * Ceiling on writes from everyone combined. Per-IP limits key on a header the
     * client controls, so one caller rotating it has no per-IP ceiling at all;
     * this is what actually bounds the damage.
     */
    private int globalWritePerMinute = 60;

    /**
     * Cap on tracked buckets. X-Forwarded-For is client-supplied, so without a
     * ceiling an attacker rotating values allocates a bucket per request.
     */
    private int maxTrackedClients = 20_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTokenMintPerMinute() {
        return tokenMintPerMinute;
    }

    public void setTokenMintPerMinute(int tokenMintPerMinute) {
        this.tokenMintPerMinute = tokenMintPerMinute;
    }

    public int getWritePerMinute() {
        return writePerMinute;
    }

    public void setWritePerMinute(int writePerMinute) {
        this.writePerMinute = writePerMinute;
    }

    public int getReadPerMinute() {
        return readPerMinute;
    }

    public void setReadPerMinute(int readPerMinute) {
        this.readPerMinute = readPerMinute;
    }

    public int getGlobalWritePerMinute() {
        return globalWritePerMinute;
    }

    public void setGlobalWritePerMinute(int globalWritePerMinute) {
        this.globalWritePerMinute = globalWritePerMinute;
    }

    public int getMaxTrackedClients() {
        return maxTrackedClients;
    }

    public void setMaxTrackedClients(int maxTrackedClients) {
        this.maxTrackedClients = maxTrackedClients;
    }
}
