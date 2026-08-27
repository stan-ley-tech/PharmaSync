package com.pharmasync.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TransferStockRequest(
        @NotNull Long medicineId,
        @NotNull Long fromPharmacyId,
        @NotNull Long toPharmacyId,
        @Min(1) int quantity) {
}
