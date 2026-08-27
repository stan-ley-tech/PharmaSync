package com.pharmasync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pharmasync.supplier")
public record SupplierProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxRetryAttempts,
        long retryBackoffMs) {
}
