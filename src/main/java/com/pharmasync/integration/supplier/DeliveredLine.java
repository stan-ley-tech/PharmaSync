package com.pharmasync.integration.supplier;

import java.time.LocalDate;

public record DeliveredLine(String sku, int quantityDelivered, String batchNumber, LocalDate expiryDate) {
}
