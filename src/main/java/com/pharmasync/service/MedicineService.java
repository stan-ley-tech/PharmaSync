package com.pharmasync.service;

import com.pharmasync.web.dto.MedicineRequest;
import com.pharmasync.web.dto.MedicineResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MedicineService {

    MedicineResponse create(MedicineRequest request);

    MedicineResponse update(Long id, MedicineRequest request);

    MedicineResponse getById(Long id);

    Page<MedicineResponse> search(String query, Pageable pageable);

    void deactivate(Long id);
}
