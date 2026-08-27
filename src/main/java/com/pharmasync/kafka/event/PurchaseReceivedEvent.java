package com.pharmasync.kafka.event;

import java.time.Instant;

public record PurchaseReceivedEvent(
        Long purchaseOrderId,
        String orderNumber,
        Long pharmacyId,
        Long supplierId,
        boolean fullyReceived,
        Instant occurredAt) {
}
