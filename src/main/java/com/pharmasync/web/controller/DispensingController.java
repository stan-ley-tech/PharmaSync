package com.pharmasync.web.controller;

import com.pharmasync.security.SecurityUser;
import com.pharmasync.service.DispensingService;
import com.pharmasync.web.dto.DispenseRequest;
import com.pharmasync.web.dto.DispensingResponse;
import com.pharmasync.web.dto.ReturnRequest;
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
@RequestMapping("/api/dispensing")
@RequiredArgsConstructor
public class DispensingController {

    private final DispensingService dispensingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public Page<DispensingResponse> findByPharmacy(@RequestParam Long pharmacyId, Pageable pageable) {
        return dispensingService.findByPharmacy(pharmacyId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public DispensingResponse getById(@PathVariable Long id) {
        return dispensingService.getById(id);
    }

    @PostMapping("/prescriptions/{prescriptionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<DispensingResponse> dispense(@PathVariable Long prescriptionId,
                                                         @Valid @RequestBody DispenseRequest request,
                                                         @AuthenticationPrincipal SecurityUser user) {
        DispensingResponse response = dispensingService.dispense(prescriptionId, request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/returns")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public DispensingResponse returnItems(@PathVariable Long id, @Valid @RequestBody ReturnRequest request,
                                           @AuthenticationPrincipal SecurityUser user) {
        return dispensingService.returnItems(id, request, user.getUserId());
    }
}
