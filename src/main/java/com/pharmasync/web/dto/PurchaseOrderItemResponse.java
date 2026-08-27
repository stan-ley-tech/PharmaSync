package com.pharmasync.web.dto;

import com.pharmasync.domain.procurement.PurchaseOrderItem;
import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        Long id,
        Long medicineId,
        String medicineName,
        int quantityOrdered,
        int quantityReceived,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public static PurchaseOrderItemResponse from(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getId(),
                item.getMedicine().getId(),
                item.getMedicine().getName(),
                item.getQuantityOrdered(),
                item.getQuantityReceived(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
