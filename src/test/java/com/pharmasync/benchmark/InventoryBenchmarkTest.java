package com.pharmasync.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmasync.AbstractIntegrationTest;
import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.catalog.MedicineForm;
import com.pharmasync.domain.inventory.Inventory;
import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.domain.prescription.Prescription;
import com.pharmasync.domain.prescription.PrescriptionItem;
import com.pharmasync.domain.prescription.PrescriptionStatus;
import com.pharmasync.repository.InventoryRepository;
import com.pharmasync.repository.MedicineRepository;
import com.pharmasync.repository.PharmacyRepository;
import com.pharmasync.repository.PrescriptionItemRepository;
import com.pharmasync.repository.PrescriptionRepository;
import com.pharmasync.service.InventoryService;
import com.pharmasync.service.ReceiveStockCommand;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Not part of the default build (excluded from Surefire by the "benchmark" tag — see pom.xml).
 * Run explicitly with:
 *
 *   ./mvnw test -Dgroups=benchmark -Dtest=InventoryBenchmarkTest
 *
 * Seeds a large catalog to show the schema holds up at scale, then hammers one hot medicine
 * with concurrent reserve+dispense pairs to measure throughput under contention. Results are
 * printed to stdout; see docs/benchmark.md for a recorded run.
 */
@Tag("benchmark")
class InventoryBenchmarkTest extends AbstractIntegrationTest {

    private static final int CATALOG_SIZE = 12_000;
    private static final int CONCURRENT_DISPENSES = 2_000;
    private static final int WORKER_THREADS = 24;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void seedsLargeCatalogAndSustainsConcurrentDispensing() throws Exception {
        Pharmacy pharmacy = pharmacyRepository.save(newPharmacy());

        long seedStart = System.nanoTime();
        seedCatalog(pharmacy.getId());
        long seedMillis = elapsedMillis(seedStart);
        System.out.printf("[benchmark] seeded %d inventory rows in %d ms%n", CATALOG_SIZE, seedMillis);

        assertThat(inventoryRepository.count()).isGreaterThanOrEqualTo(CATALOG_SIZE);

        Medicine hotMedicine = medicineRepository.save(newMedicine("BENCH-HOT"));
        inventoryService.receiveStock(new ReceiveStockCommand(
                pharmacy.getId(), hotMedicine.getId(), "BENCH-HOT-BATCH", 1_000_000,
                new BigDecimal("1.00"), LocalDate.now(), LocalDate.now().plusYears(1), null, null));

        List<Long> prescriptionItemIds = seedPrescriptionItems(pharmacy, hotMedicine, CONCURRENT_DISPENSES);

        ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);
        List<Long> latenciesNanos = new java.util.concurrent.CopyOnWriteArrayList<>();
        AtomicLong failures = new AtomicLong();

        long runStart = System.nanoTime();
        List<Future<?>> futures = new ArrayList<>();
        for (Long itemId : prescriptionItemIds) {
            futures.add(executor.submit(() -> {
                long start = System.nanoTime();
                try {
                    inventoryService.reserve(pharmacy.getId(), hotMedicine.getId(), itemId, 10, null);
                    inventoryService.consumeReservations(itemId, 10, null, null);
                    latenciesNanos.add(System.nanoTime() - start);
                } catch (RuntimeException ex) {
                    failures.incrementAndGet();
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();
        long totalMillis = elapsedMillis(runStart);

        report(totalMillis, latenciesNanos, failures.get());

        Inventory finalInventory = inventoryRepository.findByPharmacyIdAndMedicineId(pharmacy.getId(), hotMedicine.getId())
                .orElseThrow();
        assertThat(finalInventory.getQuantityReserved()).isEqualTo(0);
        assertThat(finalInventory.getQuantityOnHand())
                .isEqualTo(1_000_000 - (CONCURRENT_DISPENSES - failures.get()) * 10);
    }

    private void seedCatalog(Long pharmacyId) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO medicines (sku, name, form, unit_of_measure, unit_price, reorder_threshold, reorder_quantity) "
                        + "VALUES (?, ?, 'TABLET', 'TABLET', 1.00, 20, 100)",
                new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                        ps.setString(1, "BENCH-SKU-" + i);
                        ps.setString(2, "Benchmark Medicine " + i);
                    }

                    @Override
                    public int getBatchSize() {
                        return CATALOG_SIZE;
                    }
                });

        jdbcTemplate.update(
                "INSERT INTO inventory (pharmacy_id, medicine_id, quantity_on_hand, quantity_reserved) "
                        + "SELECT ?, id, 500, 0 FROM medicines WHERE sku LIKE 'BENCH-SKU-%'",
                pharmacyId);
    }

    private List<Long> seedPrescriptionItems(Pharmacy pharmacy, Medicine medicine, int count) {
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Prescription prescription = new Prescription();
            prescription.setPrescriptionNumber("BENCH-RX-" + i + "-" + System.nanoTime());
            prescription.setPharmacy(pharmacy);
            prescription.setPatientName("Benchmark Patient " + i);
            prescription.setIssuedDate(LocalDate.now());
            prescription.setStatus(PrescriptionStatus.CREATED);

            PrescriptionItem item = new PrescriptionItem();
            item.setMedicine(medicine);
            item.setQuantityPrescribed(10);
            prescription.addItem(item);

            prescriptionRepository.save(prescription);
            ids.add(prescriptionItemRepository.save(item).getId());
        }
        return ids;
    }

    private void report(long totalMillis, List<Long> latenciesNanos, long failures) {
        List<Long> sorted = new ArrayList<>(latenciesNanos);
        sorted.sort(Long::compareTo);
        double throughput = sorted.size() / (totalMillis / 1000.0);

        System.out.printf(
                "[benchmark] %d concurrent reserve+dispense pairs (%d workers) in %d ms — %.1f ops/sec, %d failed%n",
                sorted.size(), WORKER_THREADS, totalMillis, throughput, failures);
        if (!sorted.isEmpty()) {
            System.out.printf("[benchmark] latency p50=%.1fms p95=%.1fms p99=%.1fms max=%.1fms%n",
                    percentile(sorted, 0.50), percentile(sorted, 0.95), percentile(sorted, 0.99),
                    sorted.get(sorted.size() - 1) / 1_000_000.0);
        }
    }

    private double percentile(List<Long> sortedNanos, double p) {
        int index = Math.min(sortedNanos.size() - 1, (int) Math.ceil(p * sortedNanos.size()) - 1);
        return sortedNanos.get(Math.max(index, 0)) / 1_000_000.0;
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private Pharmacy newPharmacy() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setCode("BENCH-" + System.nanoTime());
        pharmacy.setName("Benchmark Branch");
        pharmacy.setAddressLine1("1 Benchmark Way");
        pharmacy.setCity("Testville");
        pharmacy.setCountry("Nigeria");
        return pharmacy;
    }

    private Medicine newMedicine(String skuPrefix) {
        Medicine medicine = new Medicine();
        medicine.setSku(skuPrefix + "-" + System.nanoTime());
        medicine.setName("Benchmark Hot Medicine");
        medicine.setForm(MedicineForm.TABLET);
        medicine.setUnitPrice(new BigDecimal("1.00"));
        return medicine;
    }
}
