package com.pharmasync.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record DispenseRequest(@NotEmpty @Valid List<DispenseLineRequest> items, String notes) {
}
