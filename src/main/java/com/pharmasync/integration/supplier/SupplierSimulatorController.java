package com.pharmasync.integration.supplier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Stands in for a third-party supplier system so the ordering workflow can be exercised
 * end-to-end without a real vendor integration. {@link HttpSupplierApiClient} talks to this
 * controller exactly as it would talk to an external HTTP API.
 */
@RestController
@RequestMapping("/simulator/supplier")
public class SupplierSimulatorController {

    private static final Map<String, List<SupplierCatalogItem>> CATALOGS = Map.of(
            "MEDCO", List.of(
                    new SupplierCatalogItem("PARA-500", "Paracetamol 500mg", new BigDecimal("0.0500"), 5000),
                    new SupplierCatalogItem("AMOX-250", "Amoxicillin 250mg", new BigDecimal("0.1200"), 2000),
                    new SupplierCatalogItem("IBU-200", "Ibuprofen 200mg", new BigDecimal("0.0800"), 3000)),
            "GLOBALPHARMA", List.of(
                    new SupplierCatalogItem("PARA-500", "Paracetamol 500mg", new BigDecimal("0.0450"), 8000),
                    new SupplierCatalogItem("METF-500", "Metformin 500mg", new BigDecimal("0.0900"), 1500)));

    /** Keyed by order number: remaining number of times the order endpoint should fail before succeeding. */
    private final Map<String, Integer> pendingFailures = new ConcurrentHashMap<>();

    @GetMapping("/{code}/catalog")
    public List<SupplierCatalogItem> catalog(@PathVariable String code) {
        return CATALOGS.getOrDefault(code, List.of());
    }

    @PostMapping("/{code}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierOrderResponse createOrder(@PathVariable String code,
                                              @RequestBody SupplierOrderRequest request,
                                              @RequestParam(required = false, defaultValue = "0") int simulateFailures) {
        if (simulateFailures > 0) {
            int remaining = pendingFailures.merge(request.orderNumber(), simulateFailures, (existing, incoming) -> existing);
            if (remaining > 0) {
                pendingFailures.put(request.orderNumber(), remaining - 1);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated supplier outage");
            }
        }

        String reference = "SUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new SupplierOrderResponse(reference, "ACKNOWLEDGED", Instant.now());
    }

    @PostMapping("/{code}/orders/{reference}/delivery")
    public DeliveryConfirmation confirmDelivery(@PathVariable String code,
                                                 @PathVariable String reference,
                                                 @RequestBody List<SupplierOrderLine> orderedLines) {
        List<DeliveredLine> delivered = orderedLines.stream()
                .map(line -> new DeliveredLine(
                        line.sku(),
                        line.quantity(),
                        "BATCH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                        LocalDate.now().plusYears(1)))
                .toList();
        return new DeliveryConfirmation(reference, delivered, Instant.now());
    }
}
