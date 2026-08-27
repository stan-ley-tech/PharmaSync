package com.pharmasync.web.controller;

import com.pharmasync.security.SecurityUser;
import com.pharmasync.service.PrescriptionService;
import com.pharmasync.web.dto.CreatePrescriptionRequest;
import com.pharmasync.web.dto.PrescriptionResponse;
import com.pharmasync.web.dto.RejectPrescriptionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public Page<PrescriptionResponse> findByPharmacy(@RequestParam Long pharmacyId, Pageable pageable) {
        return prescriptionService.findByPharmacy(pharmacyId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public PrescriptionResponse getById(@PathVariable Long id) {
        return prescriptionService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PHARMACIST')")
    public ResponseEntity<PrescriptionResponse> create(@Valid @RequestBody CreatePrescriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionService.create(request));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public PrescriptionResponse validate(@PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        return prescriptionService.validate(id, user.getUserId());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public PrescriptionResponse reject(@PathVariable Long id, @Valid @RequestBody RejectPrescriptionRequest request,
                                        @AuthenticationPrincipal SecurityUser user) {
        return prescriptionService.reject(id, request.reason(), user.getUserId());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public PrescriptionResponse cancel(@PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        return prescriptionService.cancel(id, user.getUserId());
    }
}
