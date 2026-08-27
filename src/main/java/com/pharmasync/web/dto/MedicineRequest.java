package com.pharmasync.web.dto;

import com.pharmasync.domain.catalog.MedicineForm;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MedicineRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String genericName,
        @NotNull MedicineForm form,
        String strength,
        String manufacturer,
        String unitOfMeasure,
        boolean requiresPrescription,
        boolean controlledSubstance,
        @Min(0) int reorderThreshold,
        @Min(0) int reorderQuantity,
        Long defaultSupplierId,
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal unitPrice) {
}
