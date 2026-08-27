package com.pharmasync.web.controller;

import com.pharmasync.service.SupplierService;
import com.pharmasync.web.dto.SupplierRequest;
import com.pharmasync.web.dto.SupplierResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public List<SupplierResponse> findAllActive() {
        return supplierService.findAllActive();
    }

    @GetMapping("/{id}")
    public SupplierResponse getById(@PathVariable Long id) {
        return supplierService.getById(id);
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        supplierService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
