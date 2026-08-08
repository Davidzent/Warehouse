package com.warehouse.receiving.web;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.warehouse.receiving.config.RateLimitProperties;
import com.warehouse.receiving.config.RateLimiter;

import tools.jackson.databind.ObjectMapper;

class RateLimitFilterTest {

    private static final String MINT = "/api/auth/dev-token";

    private RateLimitProperties properties;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setTokenMintPerMinute(2);
        properties.setWritePerMinute(2);
        properties.setReadPerMinute(3);
        filter = new RateLimitFilter(new RateLimiter(properties), properties, new ObjectMapper());
    }

    private MockHttpServletResponse call(String method, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        if (ip != null) {
            request.addHeader("X-Forwarded-For", ip);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private int status(String method, String path, String ip) throws Exception {
        return call(method, path, ip).getStatus();
    }

    @Test
    void refusesTheMintEndpointOnceTheBurstIsSpent() throws Exception {
        assertThat(status("POST", MINT, "203.0.113.1")).isEqualTo(200);
        assertThat(status("POST", MINT, "203.0.113.1")).isEqualTo(200);

        assertThat(status("POST", MINT, "203.0.113.1")).isEqualTo(429);
    }

    @Test
    void refusalIsProblemJsonWithARetryAfter() throws Exception {
        call("POST", MINT, "203.0.113.2");
        call("POST", MINT, "203.0.113.2");

        MockHttpServletResponse denied = call("POST", MINT, "203.0.113.2");

        assertThat(denied.getStatus()).isEqualTo(429);
        assertThat(denied.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(denied.getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
        assertThat(denied.getContentAsString()).contains("Too many requests");
    }

    @Test
    void addressesAreKeyedFromXForwardedForNotTheProxy() throws Exception {
        call("POST", MINT, "203.0.113.3");
        call("POST", MINT, "203.0.113.3");

        assertThat(status("POST", MINT, "203.0.113.4")).isEqualTo(200);
    }

    /** Only the leftmost entry is the client; proxy hops must not change the key. */
    @Test
    void proxyHopsInTheHeaderDoNotCreateANewBucket() throws Exception {
        call("POST", MINT, "203.0.113.7, 10.0.0.1");
        call("POST", MINT, "203.0.113.7, 10.0.0.2");

        assertThat(status("POST", MINT, "203.0.113.7, 10.0.0.3")).isEqualTo(429);
    }

    /** Reads must not be spent by the mint budget, or signing in locks a clerk out. */
    @Test
    void readsCarryTheirOwnBudget() throws Exception {
        call("POST", MINT, "203.0.113.5");
        call("POST", MINT, "203.0.113.5");
        call("POST", MINT, "203.0.113.5");

        assertThat(status("GET", "/api/locations", "203.0.113.5")).isEqualTo(200);
    }

    @Test
    void writesAndReadsDoNotShareABudget() throws Exception {
        status("POST", "/api/receipts", "203.0.113.8");
        status("POST", "/api/receipts", "203.0.113.8");
        assertThat(status("POST", "/api/receipts", "203.0.113.8")).isEqualTo(429);

        assertThat(status("GET", "/api/inventory", "203.0.113.8")).isEqualTo(200);
    }

    @Test
    void preflightIsNeverRateLimited() throws Exception {
        for (int i = 0; i < 6; i++) {
            call("POST", MINT, "203.0.113.6");
        }

        assertThat(status("OPTIONS", "/api/locations", "203.0.113.6")).isEqualTo(200);
    }

    @Test
    void disablingTheLimiterLetsEverythingThrough() throws Exception {
        properties.setEnabled(false);

        for (int i = 0; i < 20; i++) {
            assertThat(status("POST", MINT, "203.0.113.9")).isEqualTo(200);
        }
    }

    @Test
    void fallsBackToTheRemoteAddressWhenNoForwardedHeaderIsPresent() throws Exception {
        assertThat(status("POST", MINT, null)).isEqualTo(200);
        assertThat(status("POST", MINT, null)).isEqualTo(200);

        assertThat(status("POST", MINT, null)).isEqualTo(429);
    }
}
