package com.pharmasync.service.impl;

import com.pharmasync.domain.catalog.Supplier;
import com.pharmasync.exception.DuplicateResourceException;
import com.pharmasync.exception.ResourceNotFoundException;
import com.pharmasync.repository.SupplierRepository;
import com.pharmasync.service.SupplierService;
import com.pharmasync.web.dto.SupplierRequest;
import com.pharmasync.web.dto.SupplierResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        if (supplierRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("A supplier with code " + request.code() + " already exists");
        }
        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @Override
    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
        applyRequest(supplier, request);
        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return supplierRepository.findById(id)
                .map(SupplierResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> findAllActive() {
        return supplierRepository.findByActiveTrue().stream().map(SupplierResponse::from).toList();
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        supplier.setCode(request.code());
        supplier.setName(request.name());
        supplier.setContactName(request.contactName());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setApiBaseUrl(request.apiBaseUrl());
    }
}
