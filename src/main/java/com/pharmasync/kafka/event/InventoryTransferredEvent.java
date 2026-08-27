package com.pharmasync.kafka.event;

import java.time.Instant;

public record InventoryTransferredEvent(
        Long medicineId,
        Long fromPharmacyId,
        Long toPharmacyId,
        int quantity,
        Instant occurredAt) {
}
