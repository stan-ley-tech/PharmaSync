package com.pharmasync.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreatePrescriptionRequest(
        @NotNull Long pharmacyId,
        @NotBlank String patientName,
        String patientIdentifier,
        String patientContact,
        Long prescribedByUserId,
        String prescriberName,
        String prescriberLicense,
        @NotNull LocalDate issuedDate,
        @NotEmpty @Valid List<PrescriptionLineRequest> items) {
}
