package com.pharmasync.service;

import com.pharmasync.web.dto.SupplierRequest;
import com.pharmasync.web.dto.SupplierResponse;
import java.util.List;

public interface SupplierService {

    SupplierResponse create(SupplierRequest request);

    SupplierResponse update(Long id, SupplierRequest request);

    SupplierResponse getById(Long id);

    List<SupplierResponse> findAllActive();

    void deactivate(Long id);
}
