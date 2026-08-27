package com.pharmasync.kafka.event;

import java.time.Instant;
import java.time.LocalDate;

public record MedicineExpiringEvent(
        Long inventoryBatchId,
        Long medicineId,
        String medicineName,
        String batchNumber,
        int quantityRemaining,
        LocalDate expiryDate,
        Instant occurredAt) {
}
