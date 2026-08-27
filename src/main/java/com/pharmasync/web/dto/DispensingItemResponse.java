package com.pharmasync.web.dto;

import com.pharmasync.domain.dispensing.DispensingItem;
import java.math.BigDecimal;

public record DispensingItemResponse(
        Long id,
        Long prescriptionItemId,
        Long inventoryBatchId,
        int quantity,
        int quantityReturned,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public static DispensingItemResponse from(DispensingItem item) {
        return new DispensingItemResponse(
                item.getId(),
                item.getPrescriptionItem().getId(),
                item.getInventoryBatch().getId(),
                item.getQuantity(),
                item.getQuantityReturned(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
