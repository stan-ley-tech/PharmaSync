package com.pharmasync.service.impl;

import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.exception.DuplicateResourceException;
import com.pharmasync.exception.ResourceNotFoundException;
import com.pharmasync.repository.PharmacyRepository;
import com.pharmasync.service.PharmacyService;
import com.pharmasync.web.dto.PharmacyRequest;
import com.pharmasync.web.dto.PharmacyResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PharmacyServiceImpl implements PharmacyService {

    private final PharmacyRepository pharmacyRepository;

    @Override
    @Transactional
    public PharmacyResponse create(PharmacyRequest request) {
        if (pharmacyRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("A pharmacy with code " + request.code() + " already exists");
        }
        Pharmacy pharmacy = new Pharmacy();
        applyRequest(pharmacy, request);
        return PharmacyResponse.from(pharmacyRepository.save(pharmacy));
    }

    @Override
    @Transactional
    public PharmacyResponse update(Long id, PharmacyRequest request) {
        Pharmacy pharmacy = pharmacyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Pharmacy", id));
        applyRequest(pharmacy, request);
        return PharmacyResponse.from(pharmacyRepository.save(pharmacy));
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyResponse getById(Long id) {
        return pharmacyRepository.findById(id)
                .map(PharmacyResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Pharmacy", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyResponse> findAllActive() {
        return pharmacyRepository.findByActiveTrue().stream().map(PharmacyResponse::from).toList();
    }

    private void applyRequest(Pharmacy pharmacy, PharmacyRequest request) {
        pharmacy.setCode(request.code());
        pharmacy.setName(request.name());
        pharmacy.setAddressLine1(request.addressLine1());
        pharmacy.setAddressLine2(request.addressLine2());
        pharmacy.setCity(request.city());
        pharmacy.setState(request.state());
        pharmacy.setPostalCode(request.postalCode());
        pharmacy.setCountry(request.country());
        pharmacy.setPhone(request.phone());
    }
}
