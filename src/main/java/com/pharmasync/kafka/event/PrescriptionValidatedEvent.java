package com.pharmasync.kafka.event;

import java.time.Instant;

public record PrescriptionValidatedEvent(
        Long prescriptionId,
        String prescriptionNumber,
        Long pharmacyId,
        Long validatedByUserId,
        boolean approved,
        String rejectionReason,
        Instant occurredAt) {
}
