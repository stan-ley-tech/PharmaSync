package com.pharmasync.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PrescriptionLineRequest(
        @NotNull Long medicineId,
        @Min(1) int quantity,
        String dosageInstructions,
        boolean substitutionAllowed) {
}
