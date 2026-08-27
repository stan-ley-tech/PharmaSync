package com.pharmasync.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmasync.AbstractIntegrationTest;
import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.catalog.Supplier;
import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.repository.MedicineRepository;
import com.pharmasync.repository.PharmacyRepository;
import com.pharmasync.repository.SupplierRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Exercises the full documented workflow end to end against real Postgres, Redis, and Kafka
 * containers: purchase order -> supplier delivery -> prescription -> validation (reservation) ->
 * dispensing, asserting the inventory numbers at each step.
 */
class PrescriptionDispensingFlowIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    private Long pharmacyId;
    private Long medicineId;
    private Long supplierId;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Supplier medco = supplierRepository.findByCode("MEDCO").orElseThrow();
        medco.setApiBaseUrl("http://localhost:" + port + "/simulator/supplier");
        supplierRepository.save(medco);
        supplierId = medco.getId();

        Pharmacy pharmacy = pharmacyRepository.findByCode("MAIN01").orElseThrow();
        pharmacyId = pharmacy.getId();

        Medicine medicine = medicineRepository.findBySku("PARA-500").orElseThrow();
        medicineId = medicine.getId();

        adminToken = login("admin", "ChangeMe123!");
    }

    @Test
    void purchaseOrderThroughDispensing_updatesInventoryCorrectlyAtEachStep() {
        Map<String, Object> pharmacistRequest = Map.of(
                "username", "pharm-flow-1",
                "email", "pharm-flow-1@pharmasync.local",
                "password", "Password123!",
                "firstName", "Flow",
                "lastName", "Pharmacist",
                "pharmacyId", pharmacyId,
                "roles", List.of("PHARMACIST"));
        post("/api/users", pharmacistRequest, adminToken, Map.class);
        String pharmacistToken = login("pharm-flow-1", "Password123!");

        Map<String, Object> purchaseOrderRequest = Map.of(
                "pharmacyId", pharmacyId,
                "supplierId", supplierId,
                "lines", List.of(Map.of("medicineId", medicineId, "quantity", 500, "unitPrice", 0.10)));
        ResponseEntity<Map> purchaseOrderResponse = post("/api/purchase-orders", purchaseOrderRequest, adminToken, Map.class);
        assertThat(purchaseOrderResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number purchaseOrderId = (Number) purchaseOrderResponse.getBody().get("id");

        post("/api/purchase-orders/" + purchaseOrderId + "/submit", null, adminToken, Map.class);
        ResponseEntity<Map> receiveResponse =
                post("/api/purchase-orders/" + purchaseOrderId + "/receive", null, adminToken, Map.class);
        assertThat(receiveResponse.getBody().get("status")).isEqualTo("RECEIVED");

        Map<String, Object> inventoryAfterPurchase = lookupInventory();
        assertThat(inventoryAfterPurchase.get("quantityAvailable")).isEqualTo(500);

        Map<String, Object> prescriptionRequest = Map.of(
                "pharmacyId", pharmacyId,
                "patientName", "Jane Doe",
                "issuedDate", LocalDate.now().toString(),
                "items", List.of(Map.of("medicineId", medicineId, "quantity", 20, "substitutionAllowed", true)));
        ResponseEntity<Map> prescriptionResponse = post("/api/prescriptions", prescriptionRequest, pharmacistToken, Map.class);
        assertThat(prescriptionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number prescriptionId = (Number) prescriptionResponse.getBody().get("id");
        List<Map> items = (List<Map>) prescriptionResponse.getBody().get("items");
        Number prescriptionItemId = (Number) items.get(0).get("id");

        post("/api/prescriptions/" + prescriptionId + "/validate", null, pharmacistToken, Map.class);

        Map<String, Object> inventoryAfterValidate = lookupInventory();
        assertThat(inventoryAfterValidate.get("quantityAvailable")).isEqualTo(480);
        assertThat(inventoryAfterValidate.get("quantityReserved")).isEqualTo(20);

        Map<String, Object> dispenseRequest = Map.of(
                "items", List.of(Map.of("prescriptionItemId", prescriptionItemId, "quantity", 20)));
        ResponseEntity<Map> dispenseResponse =
                post("/api/dispensing/prescriptions/" + prescriptionId, dispenseRequest, pharmacistToken, Map.class);
        assertThat(dispenseResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> inventoryAfterDispense = lookupInventory();
        assertThat(inventoryAfterDispense.get("quantityOnHand")).isEqualTo(480);
        assertThat(inventoryAfterDispense.get("quantityReserved")).isEqualTo(0);
        assertThat(inventoryAfterDispense.get("quantityAvailable")).isEqualTo(480);

        ResponseEntity<Map> prescriptionAfterDispense =
                get("/api/prescriptions/" + prescriptionId, pharmacistToken, Map.class);
        assertThat(prescriptionAfterDispense.getBody().get("status")).isEqualTo("DISPENSED");
    }

    private Map<String, Object> lookupInventory() {
        ResponseEntity<Map> response = get(
                "/api/inventory/lookup?pharmacyId=" + pharmacyId + "&medicineId=" + medicineId, adminToken, Map.class);
        return response.getBody();
    }

    private String login(String username, String password) {
        ResponseEntity<Map> response = post("/api/auth/login",
                Map.of("username", username, "password", password), null, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("accessToken");
    }

    private <T> ResponseEntity<T> post(String path, Object body, String token, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers(token)), responseType);
    }

    private <T> ResponseEntity<T> get(String path, String token, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(token)), responseType);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }
}
