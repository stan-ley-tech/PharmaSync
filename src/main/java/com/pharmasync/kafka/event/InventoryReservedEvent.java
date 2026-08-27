package com.pharmasync.kafka.event;

import java.time.Instant;

public record InventoryReservedEvent(
        Long pharmacyId,
        Long medicineId,
        Long prescriptionItemId,
        int quantity,
        Instant occurredAt) {
}
