package com.pharmasync.web.controller;

import com.pharmasync.security.SecurityUser;
import com.pharmasync.service.PurchaseOrderService;
import com.pharmasync.web.dto.CreatePurchaseOrderRequest;
import com.pharmasync.web.dto.PurchaseOrderResponse;
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
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public Page<PurchaseOrderResponse> findByPharmacy(@RequestParam Long pharmacyId, Pageable pageable) {
        return purchaseOrderService.findByPharmacy(pharmacyId, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse getById(@PathVariable Long id) {
        return purchaseOrderService.getById(id);
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request,
                                                          @AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.create(request, user.getUserId()));
    }

    @PostMapping("/{id}/submit")
    public PurchaseOrderResponse submit(@PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        return purchaseOrderService.submit(id, user.getUserId());
    }

    @PostMapping("/{id}/receive")
    public PurchaseOrderResponse receiveDelivery(@PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        return purchaseOrderService.receiveDelivery(id, user.getUserId());
    }
}
