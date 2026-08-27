package com.pharmasync.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DispenseLineRequest(@NotNull Long prescriptionItemId, @Min(1) int quantity) {
}
