package com.pharmasync.web.dto;

import com.pharmasync.domain.prescription.Prescription;
import com.pharmasync.domain.prescription.PrescriptionStatus;
import java.time.LocalDate;
import java.util.List;

public record PrescriptionResponse(
        Long id,
        String prescriptionNumber,
        Long pharmacyId,
        String patientName,
        String patientIdentifier,
        String patientContact,
        String prescriberName,
        PrescriptionStatus status,
        String rejectionReason,
        LocalDate issuedDate,
        List<PrescriptionItemResponse> items) {

    public static PrescriptionResponse from(Prescription prescription) {
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPrescriptionNumber(),
                prescription.getPharmacy().getId(),
                prescription.getPatientName(),
                prescription.getPatientIdentifier(),
                prescription.getPatientContact(),
                prescription.getPrescriberName(),
                prescription.getStatus(),
                prescription.getRejectionReason(),
                prescription.getIssuedDate(),
                prescription.getItems().stream().map(PrescriptionItemResponse::from).toList());
    }
}
