package com.pharmasync.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectPrescriptionRequest(@NotBlank String reason) {
}
