package com.pharmasync.service;

import com.pharmasync.web.dto.PharmacyRequest;
import com.pharmasync.web.dto.PharmacyResponse;
import java.util.List;

public interface PharmacyService {

    PharmacyResponse create(PharmacyRequest request);

    PharmacyResponse update(Long id, PharmacyRequest request);

    PharmacyResponse getById(Long id);

    List<PharmacyResponse> findAllActive();
}
