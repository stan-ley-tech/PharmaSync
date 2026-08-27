package com.pharmasync.kafka.event;

import java.time.Instant;

public record PrescriptionCreatedEvent(
        Long prescriptionId,
        String prescriptionNumber,
        Long pharmacyId,
        String patientName,
        Instant occurredAt) {
}
