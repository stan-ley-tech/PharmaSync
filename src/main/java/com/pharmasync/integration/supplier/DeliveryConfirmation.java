package com.pharmasync.integration.supplier;

import java.time.Instant;
import java.util.List;

public record DeliveryConfirmation(String supplierReference, List<DeliveredLine> lines, Instant deliveredAt) {
}
