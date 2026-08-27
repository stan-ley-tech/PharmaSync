package com.pharmasync.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderRequest(
        @NotNull Long pharmacyId,
        @NotNull Long supplierId,
        LocalDate expectedDeliveryDate,
        String notes,
        @NotEmpty @Valid List<PurchaseOrderLineRequest> lines) {
}
