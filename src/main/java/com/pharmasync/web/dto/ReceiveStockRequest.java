package com.pharmasync.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Manual goods-in, for stock that doesn't arrive through a tracked purchase order. */
public record ReceiveStockRequest(
        @NotNull Long pharmacyId,
        @NotNull Long medicineId,
        @NotBlank String batchNumber,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0") BigDecimal unitCost,
        LocalDate manufacturedDate,
        @NotNull LocalDate expiryDate) {
}
