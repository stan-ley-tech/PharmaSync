package com.pharmasync.service;

import com.pharmasync.web.dto.CreatePurchaseOrderRequest;
import com.pharmasync.web.dto.PurchaseOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderService {

    PurchaseOrderResponse create(CreatePurchaseOrderRequest request, Long createdByUserId);

    PurchaseOrderResponse submit(Long purchaseOrderId, Long performedByUserId);

    PurchaseOrderResponse receiveDelivery(Long purchaseOrderId, Long performedByUserId);

    PurchaseOrderResponse getById(Long id);

    Page<PurchaseOrderResponse> findByPharmacy(Long pharmacyId, Pageable pageable);
}
