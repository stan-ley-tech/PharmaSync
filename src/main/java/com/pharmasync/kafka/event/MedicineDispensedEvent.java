package com.pharmasync.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record MedicineDispensedEvent(
        Long dispensingId,
        String dispensingNumber,
        Long prescriptionId,
        Long pharmacyId,
        Long dispensedByUserId,
        BigDecimal totalAmount,
        Instant occurredAt) {
}
