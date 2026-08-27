package com.pharmasync.web.dto;

import com.pharmasync.domain.prescription.PrescriptionItem;

public record PrescriptionItemResponse(
        Long id,
        Long medicineId,
        String medicineName,
        int quantityPrescribed,
        int quantityDispensed,
        String dosageInstructions,
        boolean substitutionAllowed) {

    public static PrescriptionItemResponse from(PrescriptionItem item) {
        return new PrescriptionItemResponse(
                item.getId(),
                item.getMedicine().getId(),
                item.getMedicine().getName(),
                item.getQuantityPrescribed(),
                item.getQuantityDispensed(),
                item.getDosageInstructions(),
                item.isSubstitutionAllowed());
    }
}
