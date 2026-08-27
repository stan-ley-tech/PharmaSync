package com.pharmasync.service;

import com.pharmasync.web.dto.DispenseRequest;
import com.pharmasync.web.dto.DispensingResponse;
import com.pharmasync.web.dto.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DispensingService {

    DispensingResponse dispense(Long prescriptionId, DispenseRequest request, Long dispensedByUserId);

    DispensingResponse returnItems(Long dispensingId, ReturnRequest request, Long performedByUserId);

    DispensingResponse getById(Long id);

    Page<DispensingResponse> findByPharmacy(Long pharmacyId, Pageable pageable);
}
