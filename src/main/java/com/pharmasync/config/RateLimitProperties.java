package com.pharmasync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pharmasync.rate-limit")
public record RateLimitProperties(
        long capacity,
        long refillTokens,
        long refillDurationSeconds) {
}
