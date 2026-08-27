package com.pharmasync.integration.supplier;

import java.math.BigDecimal;

public record SupplierCatalogItem(String sku, String name, BigDecimal unitPrice, int availableQuantity) {
}
