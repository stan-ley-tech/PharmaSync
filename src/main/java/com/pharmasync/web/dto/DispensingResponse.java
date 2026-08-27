package com.pharmasync.web.dto;

import com.pharmasync.domain.dispensing.Dispensing;
import com.pharmasync.domain.dispensing.DispensingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DispensingResponse(
        Long id,
        String dispensingNumber,
        Long prescriptionId,
        Long pharmacyId,
        Long dispensedByUserId,
        DispensingStatus status,
        BigDecimal totalAmount,
        Instant dispensedAt,
        List<DispensingItemResponse> items) {

    public static DispensingResponse from(Dispensing dispensing) {
        return new DispensingResponse(
                dispensing.getId(),
                dispensing.getDispensingNumber(),
                dispensing.getPrescription().getId(),
                dispensing.getPharmacy().getId(),
                dispensing.getDispensedBy().getId(),
                dispensing.getStatus(),
                dispensing.getTotalAmount(),
                dispensing.getDispensedAt(),
                dispensing.getItems().stream().map(DispensingItemResponse::from).toList());
    }
}
