package com.pharmasync.integration.supplier;

import java.util.List;

/**
 * Adapter boundary to an external supplier system. The base URL is resolved per supplier
 * so each vendor can, in principle, expose its own ordering API.
 */
public interface SupplierApiClient {

    List<SupplierCatalogItem> fetchCatalog(String baseUrl, String supplierCode);

    SupplierOrderResponse submitOrder(String baseUrl, String supplierCode, SupplierOrderRequest request);

    DeliveryConfirmation confirmDelivery(String baseUrl, String supplierCode, String supplierReference,
                                          List<SupplierOrderLine> orderedLines);
}
