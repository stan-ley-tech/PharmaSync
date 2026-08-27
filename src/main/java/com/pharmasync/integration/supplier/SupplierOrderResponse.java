package com.pharmasync.integration.supplier;

import java.time.Instant;

public record SupplierOrderResponse(String supplierReference, String status, Instant acknowledgedAt) {
}
