package com.warehouse.receiving.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import com.warehouse.receiving.config.RateLimiter;
import com.warehouse.receiving.config.RateLimiter.Tier;
import com.warehouse.receiving.config.RateLimitProperties;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Refuses requests over the per-IP rate with 429 and a Retry-After. */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiter rateLimiter,
                           RateLimitProperties properties,
                           ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflights do no work worth metering, and counting them would tie the
        // limit to whether the browser had one cached.
        return !properties.isEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Tier tier = tierFor(request);
        String clientId = clientId(request);
        RateLimiter.Decision decision = rateLimiter.check(clientId, tier);

        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit hit: tier={} client={} path={}", tier, clientId, request.getRequestURI());
        reject(response, decision.retryAfterSeconds());
    }

    /** Prefix match: this runs before MVC resolves a handler. */
    private Tier tierFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            return Tier.TOKEN_MINT;
        }
        return "GET".equalsIgnoreCase(request.getMethod()) ? Tier.READ : Tier.WRITE;
    }

    /**
     * Behind Render and Cloudflare getRemoteAddr() is a proxy, so every visitor
     * would share one bucket. X-Forwarded-For is spoofable; that is bounded by
     * maxTrackedClients rather than solved, which would need a proxy allowlist.
     */
    private String clientId(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
            if (!first.isEmpty()) {
                // Header is attacker-controlled; cap what one map key can cost.
                return first.length() > 45 ? first.substring(0, 45) : first;
            }
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }

    /** Built by hand: a filter runs outside @RestControllerAdvice. */
    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Wait %d second(s) and try again.".formatted(retryAfterSeconds));
        problem.setTitle("Too Many Requests");

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
