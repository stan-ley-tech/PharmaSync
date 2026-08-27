package com.pharmasync.kafka.event;

import java.time.Instant;

public record InventoryLowEvent(
        Long pharmacyId,
        Long medicineId,
        String medicineName,
        int quantityAvailable,
        int reorderThreshold,
        Instant occurredAt) {
}
