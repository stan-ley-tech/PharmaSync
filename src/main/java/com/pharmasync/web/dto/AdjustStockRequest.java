package com.pharmasync.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(@NotNull Integer delta, @NotBlank String reason) {
}
