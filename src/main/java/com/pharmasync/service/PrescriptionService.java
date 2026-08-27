package com.pharmasync.service;

import com.pharmasync.web.dto.CreatePrescriptionRequest;
import com.pharmasync.web.dto.PrescriptionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PrescriptionService {

    PrescriptionResponse create(CreatePrescriptionRequest request);

    PrescriptionResponse validate(Long prescriptionId, Long validatedByUserId);

    PrescriptionResponse reject(Long prescriptionId, String reason, Long validatedByUserId);

    PrescriptionResponse cancel(Long prescriptionId, Long performedByUserId);

    PrescriptionResponse getById(Long id);

    Page<PrescriptionResponse> findByPharmacy(Long pharmacyId, Pageable pageable);
}
