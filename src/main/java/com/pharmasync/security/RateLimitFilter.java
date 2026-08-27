package com.pharmasync.security;

import com.pharmasync.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies a per-client token bucket so a single API key/IP cannot exhaust the service.
 * Buckets are kept in memory, which is sufficient for a single-instance deployment;
 * a multi-instance deployment would back this with the shared Redis cache instead.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Bucket bucket = buckets.computeIfAbsent(resolveClientKey(request), key -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Rate limit exceeded, please slow down\"}");
        }
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(properties.capacity(),
                Refill.greedy(properties.refillTokens(), Duration.ofSeconds(properties.refillDurationSeconds())));
        return Bucket.builder().addLimit(limit).build();
    }
}
