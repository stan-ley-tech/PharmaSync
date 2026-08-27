package com.pharmasync.web.dto;

import com.pharmasync.domain.procurement.PurchaseOrder;
import com.pharmasync.domain.procurement.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String orderNumber,
        Long pharmacyId,
        Long supplierId,
        PurchaseOrderStatus status,
        Instant submittedAt,
        LocalDate expectedDeliveryDate,
        BigDecimal totalAmount,
        String supplierReference,
        String notes,
        List<PurchaseOrderItemResponse> items) {

    public static PurchaseOrderResponse from(PurchaseOrder order) {
        return new PurchaseOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getPharmacy().getId(),
                order.getSupplier().getId(),
                order.getStatus(),
                order.getSubmittedAt(),
                order.getExpectedDeliveryDate(),
                order.getTotalAmount(),
                order.getSupplierReference(),
                order.getNotes(),
                List.of());
    }

    public static PurchaseOrderResponse from(PurchaseOrder order, List<PurchaseOrderItemResponse> items) {
        return new PurchaseOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getPharmacy().getId(),
                order.getSupplier().getId(),
                order.getStatus(),
                order.getSubmittedAt(),
                order.getExpectedDeliveryDate(),
                order.getTotalAmount(),
                order.getSupplierReference(),
                order.getNotes(),
                items);
    }
}
