package com.pharmasync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pharmasync.inventory")
public record InventoryProperties(
        String lowStockCheckCron,
        String expiryCheckCron,
        int expiryWarningDays,
        long reservationTtlMinutes) {
}
