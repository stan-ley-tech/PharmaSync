package com.pharmasync.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiveStockCommand(
        Long pharmacyId,
        Long medicineId,
        String batchNumber,
        int quantity,
        BigDecimal unitCost,
        LocalDate manufacturedDate,
        LocalDate expiryDate,
        Long purchaseOrderItemId,
        Long performedByUserId) {
}
