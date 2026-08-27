package com.pharmasync.integration.supplier;

import java.util.List;

public record SupplierOrderRequest(String orderNumber, List<SupplierOrderLine> lines) {
}
