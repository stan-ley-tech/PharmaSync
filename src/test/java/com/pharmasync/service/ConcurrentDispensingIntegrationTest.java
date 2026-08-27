package com.pharmasync.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmasync.AbstractIntegrationTest;
import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.catalog.MedicineForm;
import com.pharmasync.domain.inventory.Inventory;
import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.domain.prescription.Prescription;
import com.pharmasync.domain.prescription.PrescriptionItem;
import com.pharmasync.domain.prescription.PrescriptionStatus;
import com.pharmasync.exception.InsufficientStockException;
import com.pharmasync.repository.InventoryRepository;
import com.pharmasync.repository.MedicineRepository;
import com.pharmasync.repository.PharmacyRepository;
import com.pharmasync.repository.PrescriptionItemRepository;
import com.pharmasync.repository.PrescriptionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves that two pharmacists reserving stock for the same medicine at the same branch at the
 * same time cannot over-commit it: with 100 units on hand and two concurrent requests for 60
 * each, exactly one must succeed and the other must be rejected — never both, and never a
 * silently short allocation.
 */
class ConcurrentDispensingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Test
    void concurrentReservations_neverOverAllocateSharedStock() throws Exception {
        Pharmacy pharmacy = pharmacyRepository.save(newPharmacy());
        Medicine medicine = medicineRepository.save(newMedicine());

        inventoryService.receiveStock(new ReceiveStockCommand(
                pharmacy.getId(), medicine.getId(), "CONC-BATCH-1", 100,
                new BigDecimal("1.00"), LocalDate.now(), LocalDate.now().plusYears(1), null, null));

        Long itemA = newPrescriptionItem(pharmacy, medicine, "CONC-RX-A").getId();
        Long itemB = newPrescriptionItem(pharmacy, medicine, "CONC-RX-B").getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        List<Future<?>> futures = List.of(
                executor.submit(() -> attemptReserve(pharmacy, medicine, itemA, startLine, successCount, failureCount)),
                executor.submit(() -> attemptReserve(pharmacy, medicine, itemB, startLine, successCount, failureCount)));

        startLine.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        Inventory inventory = inventoryRepository.findByPharmacyIdAndMedicineId(pharmacy.getId(), medicine.getId())
                .orElseThrow();
        assertThat(inventory.getQuantityReserved()).isEqualTo(60);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(40);
    }

    private void attemptReserve(Pharmacy pharmacy, Medicine medicine, Long prescriptionItemId,
                                 CountDownLatch startLine, AtomicInteger successCount, AtomicInteger failureCount) {
        try {
            startLine.await();
            inventoryService.reserve(pharmacy.getId(), medicine.getId(), prescriptionItemId, 60, null);
            successCount.incrementAndGet();
        } catch (InsufficientStockException expected) {
            failureCount.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Pharmacy newPharmacy() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setCode("CONC-" + System.nanoTime());
        pharmacy.setName("Concurrency Test Branch");
        pharmacy.setAddressLine1("1 Test Street");
        pharmacy.setCity("Testville");
        pharmacy.setCountry("Nigeria");
        return pharmacy;
    }

    private Medicine newMedicine() {
        Medicine medicine = new Medicine();
        medicine.setSku("CONC-SKU-" + System.nanoTime());
        medicine.setName("Concurrency Test Medicine");
        medicine.setForm(MedicineForm.TABLET);
        medicine.setUnitPrice(new BigDecimal("2.00"));
        return medicine;
    }

    private PrescriptionItem newPrescriptionItem(Pharmacy pharmacy, Medicine medicine, String prescriptionNumber) {
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNumber(prescriptionNumber + "-" + System.nanoTime());
        prescription.setPharmacy(pharmacy);
        prescription.setPatientName("Concurrency Test Patient");
        prescription.setIssuedDate(LocalDate.now());
        prescription.setStatus(PrescriptionStatus.CREATED);

        PrescriptionItem item = new PrescriptionItem();
        item.setMedicine(medicine);
        item.setQuantityPrescribed(60);
        prescription.addItem(item);

        prescriptionRepository.save(prescription);
        return prescriptionItemRepository.save(item);
    }
}
