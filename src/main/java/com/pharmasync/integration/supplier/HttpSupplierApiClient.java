package com.pharmasync.integration.supplier;

import com.pharmasync.config.SupplierProperties;
import com.pharmasync.exception.SupplierIntegrationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class HttpSupplierApiClient implements SupplierApiClient {

    private static final Logger log = LoggerFactory.getLogger(HttpSupplierApiClient.class);

    private final SupplierProperties properties;

    @Override
    @Retryable(retryFor = RestClientException.class,
            maxAttemptsExpression = "#{@supplierProperties.maxRetryAttempts}",
            backoff = @Backoff(delayExpression = "#{@supplierProperties.retryBackoffMs}"))
    public List<SupplierCatalogItem> fetchCatalog(String baseUrl, String supplierCode) {
        SupplierCatalogItem[] items = client(baseUrl).get()
                .uri("/{code}/catalog", supplierCode)
                .retrieve()
                .body(SupplierCatalogItem[].class);
        return items != null ? List.of(items) : List.of();
    }

    @Recover
    public List<SupplierCatalogItem> recoverFetchCatalog(RestClientException ex, String baseUrl, String supplierCode) {
        log.warn("Exhausted retries fetching catalog for supplier {}", supplierCode, ex);
        throw new SupplierIntegrationException("Unable to reach supplier " + supplierCode + " for catalog sync", ex);
    }

    @Override
    @Retryable(retryFor = RestClientException.class,
            maxAttemptsExpression = "#{@supplierProperties.maxRetryAttempts}",
            backoff = @Backoff(delayExpression = "#{@supplierProperties.retryBackoffMs}"))
    public SupplierOrderResponse submitOrder(String baseUrl, String supplierCode, SupplierOrderRequest request) {
        return client(baseUrl).post()
                .uri("/{code}/orders", supplierCode)
                .body(request)
                .retrieve()
                .body(SupplierOrderResponse.class);
    }

    @Recover
    public SupplierOrderResponse recoverSubmitOrder(RestClientException ex, String baseUrl, String supplierCode,
                                                     SupplierOrderRequest request) {
        log.warn("Exhausted retries submitting order {} to supplier {}", request.orderNumber(), supplierCode, ex);
        throw new SupplierIntegrationException("Unable to submit purchase order to supplier " + supplierCode, ex);
    }

    @Override
    @Retryable(retryFor = RestClientException.class,
            maxAttemptsExpression = "#{@supplierProperties.maxRetryAttempts}",
            backoff = @Backoff(delayExpression = "#{@supplierProperties.retryBackoffMs}"))
    public DeliveryConfirmation confirmDelivery(String baseUrl, String supplierCode, String supplierReference,
                                                 List<SupplierOrderLine> orderedLines) {
        return client(baseUrl).post()
                .uri("/{code}/orders/{reference}/delivery", supplierCode, supplierReference)
                .body(orderedLines)
                .retrieve()
                .body(DeliveryConfirmation.class);
    }

    @Recover
    public DeliveryConfirmation recoverConfirmDelivery(RestClientException ex, String baseUrl, String supplierCode,
                                                        String supplierReference, List<SupplierOrderLine> orderedLines) {
        log.warn("Exhausted retries confirming delivery {} from supplier {}", supplierReference, supplierCode, ex);
        throw new SupplierIntegrationException("Unable to confirm delivery from supplier " + supplierCode, ex);
    }

    private RestClient client(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
