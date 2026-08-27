package com.pharmasync.service.impl;

import com.pharmasync.config.RedisCacheConfig;
import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.catalog.Supplier;
import com.pharmasync.exception.DuplicateResourceException;
import com.pharmasync.exception.ResourceNotFoundException;
import com.pharmasync.repository.MedicineRepository;
import com.pharmasync.repository.SupplierRepository;
import com.pharmasync.service.MedicineService;
import com.pharmasync.web.dto.MedicineRequest;
import com.pharmasync.web.dto.MedicineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.MEDICINE_SEARCH_CACHE, allEntries = true)
    public MedicineResponse create(MedicineRequest request) {
        if (medicineRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("A medicine with SKU " + request.sku() + " already exists");
        }

        Medicine medicine = new Medicine();
        applyRequest(medicine, request);
        return MedicineResponse.from(medicineRepository.save(medicine));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.MEDICINES_CACHE, key = "#id"),
            @CacheEvict(value = RedisCacheConfig.MEDICINE_SEARCH_CACHE, allEntries = true)
    })
    public MedicineResponse update(Long id, MedicineRequest request) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Medicine", id));
        applyRequest(medicine, request);
        return MedicineResponse.from(medicineRepository.save(medicine));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisCacheConfig.MEDICINES_CACHE, key = "#id")
    public MedicineResponse getById(Long id) {
        return medicineRepository.findById(id)
                .map(MedicineResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Medicine", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MedicineResponse> search(String query, Pageable pageable) {
        return medicineRepository.search(query, pageable).map(MedicineResponse::from);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.MEDICINES_CACHE, key = "#id"),
            @CacheEvict(value = RedisCacheConfig.MEDICINE_SEARCH_CACHE, allEntries = true)
    })
    public void deactivate(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Medicine", id));
        medicine.setActive(false);
        medicineRepository.save(medicine);
    }

    private void applyRequest(Medicine medicine, MedicineRequest request) {
        medicine.setSku(request.sku());
        medicine.setName(request.name());
        medicine.setGenericName(request.genericName());
        medicine.setForm(request.form());
        medicine.setStrength(request.strength());
        medicine.setManufacturer(request.manufacturer());
        medicine.setUnitOfMeasure(request.unitOfMeasure() != null ? request.unitOfMeasure() : "UNIT");
        medicine.setRequiresPrescription(request.requiresPrescription());
        medicine.setControlledSubstance(request.controlledSubstance());
        medicine.setReorderThreshold(request.reorderThreshold());
        medicine.setReorderQuantity(request.reorderQuantity());
        medicine.setUnitPrice(request.unitPrice());

        if (request.defaultSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.defaultSupplierId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Supplier", request.defaultSupplierId()));
            medicine.setDefaultSupplier(supplier);
        } else {
            medicine.setDefaultSupplier(null);
        }
    }
}
