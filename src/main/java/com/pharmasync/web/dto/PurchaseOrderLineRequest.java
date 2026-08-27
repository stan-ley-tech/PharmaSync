package com.pharmasync.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PurchaseOrderLineRequest(
        @NotNull Long medicineId,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0") BigDecimal unitPrice) {
}
