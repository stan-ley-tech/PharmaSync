package com.pharmasync.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReturnRequest(@NotNull Long dispensingItemId, @Min(1) int quantity) {
}
