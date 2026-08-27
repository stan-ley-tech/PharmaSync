package com.pharmasync.service.impl;

import com.pharmasync.config.SupplierProperties;
import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.catalog.Supplier;
import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.domain.procurement.PurchaseOrder;
import com.pharmasync.domain.procurement.PurchaseOrderItem;
import com.pharmasync.domain.procurement.PurchaseOrderStatus;
import com.pharmasync.domain.user.User;
import com.pharmasync.exception.InvalidStateTransitionException;
import com.pharmasync.exception.ResourceNotFoundException;
import com.pharmasync.integration.supplier.DeliveredLine;
import com.pharmasync.integration.supplier.DeliveryConfirmation;
import com.pharmasync.integration.supplier.SupplierApiClient;
import com.pharmasync.integration.supplier.SupplierOrderLine;
import com.pharmasync.integration.supplier.SupplierOrderRequest;
import com.pharmasync.integration.supplier.SupplierOrderResponse;
import com.pharmasync.kafka.EventPublisher;
import com.pharmasync.kafka.event.PurchaseReceivedEvent;
import com.pharmasync.repository.MedicineRepository;
import com.pharmasync.repository.PharmacyRepository;
import com.pharmasync.repository.PurchaseOrderItemRepository;
import com.pharmasync.repository.PurchaseOrderRepository;
import com.pharmasync.repository.SupplierRepository;
import com.pharmasync.repository.UserRepository;
import com.pharmasync.service.PurchaseOrderService;
import com.pharmasync.service.ReceiveStockCommand;
import com.pharmasync.service.InventoryService;
import com.pharmasync.web.dto.CreatePurchaseOrderRequest;
import com.pharmasync.web.dto.PurchaseOrderItemResponse;
import com.pharmasync.web.dto.PurchaseOrderLineRequest;
import com.pharmasync.web.dto.PurchaseOrderResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PharmacyRepository pharmacyRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final SupplierApiClient supplierApiClient;
    private final SupplierProperties supplierProperties;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request, Long createdByUserId) {
        Pharmacy pharmacy = pharmacyRepository.findById(request.pharmacyId())
                .orElseThrow(() -> ResourceNotFoundException.of("Pharmacy", request.pharmacyId()));
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", request.supplierId()));
        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", createdByUserId));

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setPharmacy(pharmacy);
        order.setSupplier(supplier);
        order.setCreatedBy(createdBy);
        order.setStatus(PurchaseOrderStatus.DRAFT);
        order.setExpectedDeliveryDate(request.expectedDeliveryDate());
        order.setNotes(request.notes());
        order = purchaseOrderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderLineRequest line : request.lines()) {
            Medicine medicine = medicineRepository.findById(line.medicineId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Medicine", line.medicineId()));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(order);
            item.setMedicine(medicine);
            item.setQuantityOrdered(line.quantity());
            item.setUnitPrice(line.unitPrice());
            item.setLineTotal(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
            purchaseOrderItemRepository.save(item);

            total = total.add(item.getLineTotal());
        }

        order.setTotalAmount(total);
        order = purchaseOrderRepository.save(order);

        return toResponse(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse submit(Long purchaseOrderId, Long performedByUserId) {
        PurchaseOrder order = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> ResourceNotFoundException.of("PurchaseOrder", purchaseOrderId));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new InvalidStateTransitionException("Only draft purchase orders can be submitted");
        }

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(purchaseOrderId);
        List<SupplierOrderLine> lines = items.stream()
                .map(item -> new SupplierOrderLine(item.getMedicine().getSku(), item.getQuantityOrdered()))
                .toList();

        Supplier supplier = order.getSupplier();
        String baseUrl = resolveBaseUrl(supplier);

        SupplierOrderResponse response;
        try {
            response = supplierApiClient.submitOrder(baseUrl, supplier.getCode(),
                    new SupplierOrderRequest(order.getOrderNumber(), lines));
        } catch (RuntimeException ex) {
            order.setStatus(PurchaseOrderStatus.FAILED);
            purchaseOrderRepository.save(order);
            throw ex;
        }

        order.setStatus(PurchaseOrderStatus.SUBMITTED);
        order.setSupplierReference(response.supplierReference());
        order.setSubmittedAt(Instant.now());
        order = purchaseOrderRepository.save(order);

        return toResponse(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse receiveDelivery(Long purchaseOrderId, Long performedByUserId) {
        PurchaseOrder order = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> ResourceNotFoundException.of("PurchaseOrder", purchaseOrderId));

        if (order.getStatus() != PurchaseOrderStatus.SUBMITTED
                && order.getStatus() != PurchaseOrderStatus.ACKNOWLEDGED
                && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new InvalidStateTransitionException("Purchase order is not awaiting delivery");
        }

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(purchaseOrderId);
        Map<String, PurchaseOrderItem> itemsBySku = items.stream()
                .collect(Collectors.toMap(item -> item.getMedicine().getSku(), item -> item));

        List<SupplierOrderLine> outstandingLines = items.stream()
                .filter(item -> item.getQuantityOutstanding() > 0)
                .map(item -> new SupplierOrderLine(item.getMedicine().getSku(), item.getQuantityOutstanding()))
                .toList();

        Supplier supplier = order.getSupplier();
        String baseUrl = resolveBaseUrl(supplier);
        DeliveryConfirmation confirmation = supplierApiClient.confirmDelivery(
                baseUrl, supplier.getCode(), order.getSupplierReference(), outstandingLines);

        for (DeliveredLine delivered : confirmation.lines()) {
            PurchaseOrderItem item = itemsBySku.get(delivered.sku());
            if (item == null || delivered.quantityDelivered() <= 0) {
                continue;
            }

            inventoryService.receiveStock(new ReceiveStockCommand(
                    order.getPharmacy().getId(),
                    item.getMedicine().getId(),
                    delivered.batchNumber(),
                    delivered.quantityDelivered(),
                    item.getUnitPrice(),
                    LocalDate.now(),
                    delivered.expiryDate(),
                    item.getId(),
                    performedByUserId));

            item.setQuantityReceived(item.getQuantityReceived() + delivered.quantityDelivered());
            purchaseOrderItemRepository.save(item);
        }

        boolean fullyReceived = items.stream().allMatch(item -> item.getQuantityOutstanding() == 0);
        order.setStatus(fullyReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
        order = purchaseOrderRepository.save(order);

        eventPublisher.publish(new PurchaseReceivedEvent(order.getId(), order.getOrderNumber(),
                order.getPharmacy().getId(), supplier.getId(), fullyReceived, Instant.now()));

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("PurchaseOrder", id));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> findByPharmacy(Long pharmacyId, Pageable pageable) {
        return purchaseOrderRepository.findByPharmacyId(pharmacyId, pageable).map(this::toResponse);
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {
        List<PurchaseOrderItemResponse> items = purchaseOrderItemRepository.findByPurchaseOrderId(order.getId())
                .stream().map(PurchaseOrderItemResponse::from).toList();
        return PurchaseOrderResponse.from(order, items);
    }

    private String resolveBaseUrl(Supplier supplier) {
        return supplier.getApiBaseUrl() != null && !supplier.getApiBaseUrl().isBlank()
                ? supplier.getApiBaseUrl()
                : supplierProperties.baseUrl();
    }

    private String generateOrderNumber() {
        return "PO-" + LocalDate.now().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
