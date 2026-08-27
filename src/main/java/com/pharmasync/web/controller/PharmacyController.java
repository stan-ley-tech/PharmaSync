package com.pharmasync.web.controller;

import com.pharmasync.service.PharmacyService;
import com.pharmasync.web.dto.PharmacyRequest;
import com.pharmasync.web.dto.PharmacyResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pharmacies")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @GetMapping
    public List<PharmacyResponse> findAllActive() {
        return pharmacyService.findAllActive();
    }

    @GetMapping("/{id}")
    public PharmacyResponse getById(@PathVariable Long id) {
        return pharmacyService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PharmacyResponse> create(@Valid @RequestBody PharmacyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pharmacyService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PharmacyResponse update(@PathVariable Long id, @Valid @RequestBody PharmacyRequest request) {
        return pharmacyService.update(id, request);
    }
}
