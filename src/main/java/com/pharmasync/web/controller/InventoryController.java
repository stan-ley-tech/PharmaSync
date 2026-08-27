package com.pharmasync.web.controller;

import com.pharmasync.repository.StockMovementRepository;
import com.pharmasync.security.SecurityUser;
import com.pharmasync.service.InventoryService;
import com.pharmasync.service.ReceiveStockCommand;
import com.pharmasync.web.dto.AdjustStockRequest;
import com.pharmasync.web.dto.InventoryResponse;
import com.pharmasync.web.dto.ReceiveStockRequest;
import com.pharmasync.web.dto.StockMovementResponse;
import com.pharmasync.web.dto.TransferStockRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final StockMovementRepository stockMovementRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'INVENTORY_MANAGER', 'AUDITOR')")
    public Page<InventoryResponse> findByPharmacy(@RequestParam Long pharmacyId, Pageable pageable) {
        return inventoryService.findByPharmacy(pharmacyId, pageable).map(InventoryResponse::from);
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'INVENTORY_MANAGER', 'AUDITOR')")
    public InventoryResponse lookup(@RequestParam Long pharmacyId, @RequestParam Long medicineId) {
        return InventoryResponse.from(inventoryService.getByPharmacyAndMedicine(pharmacyId, medicineId));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public List<InventoryResponse> lowStock() {
        return inventoryService.findLowStockInventory().stream().map(InventoryResponse::from).toList();
    }

    @GetMapping("/batches/{batchId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'AUDITOR')")
    public Page<StockMovementResponse> movements(@PathVariable Long batchId, Pageable pageable) {
        return stockMovementRepository.findByInventoryBatchIdOrderByCreatedAtDesc(batchId, pageable)
                .map(StockMovementResponse::from);
    }

    @PostMapping("/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<InventoryResponse> receive(@Valid @RequestBody ReceiveStockRequest request,
                                                       @AuthenticationPrincipal SecurityUser user) {
        inventoryService.receiveStock(new ReceiveStockCommand(
                request.pharmacyId(), request.medicineId(), request.batchNumber(), request.quantity(),
                request.unitCost(), request.manufacturedDate(), request.expiryDate(), null, user.getUserId()));
        InventoryResponse response = InventoryResponse.from(
                inventoryService.getByPharmacyAndMedicine(request.pharmacyId(), request.medicineId()));
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batches/{batchId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<Void> adjust(@PathVariable Long batchId, @Valid @RequestBody AdjustStockRequest request,
                                        @AuthenticationPrincipal SecurityUser user) {
        inventoryService.adjustStock(batchId, request.delta(), request.reason(), user.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferStockRequest request,
                                          @AuthenticationPrincipal SecurityUser user) {
        inventoryService.transferStock(request.medicineId(), request.fromPharmacyId(), request.toPharmacyId(),
                request.quantity(), user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
