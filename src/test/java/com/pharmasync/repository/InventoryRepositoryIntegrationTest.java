package com.pharmasync.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmasync.AbstractIntegrationTest;
import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.catalog.MedicineForm;
import com.pharmasync.domain.inventory.Inventory;
import com.pharmasync.domain.pharmacy.Pharmacy;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

class InventoryRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @Transactional
    void lockByPharmacyIdAndMedicineId_returnsMatchingRow() {
        Pharmacy pharmacy = pharmacyRepository.save(testPharmacy("REPO01"));
        Medicine medicine = medicineRepository.save(testMedicine("REPO-SKU-1"));

        Inventory inventory = new Inventory();
        inventory.setPharmacy(pharmacy);
        inventory.setMedicine(medicine);
        inventory.setQuantityOnHand(50);
        inventory.setQuantityReserved(10);
        inventoryRepository.saveAndFlush(inventory);

        var found = inventoryRepository.lockByPharmacyIdAndMedicineId(pharmacy.getId(), medicine.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getQuantityAvailable()).isEqualTo(40);
    }

    @Test
    @Transactional
    void findAllBelowReorderThreshold_includesRowsAtOrBelowTheirThreshold() {
        Pharmacy pharmacy = pharmacyRepository.save(testPharmacy("REPO02"));
        Medicine medicine = medicineRepository.save(testMedicine("REPO-SKU-2"));
        medicine.setReorderThreshold(20);
        medicineRepository.save(medicine);

        Inventory lowStock = new Inventory();
        lowStock.setPharmacy(pharmacy);
        lowStock.setMedicine(medicine);
        lowStock.setQuantityOnHand(20);
        lowStock.setQuantityReserved(5); // available = 15, below threshold of 20
        inventoryRepository.saveAndFlush(lowStock);

        assertThat(inventoryRepository.findAllBelowReorderThreshold())
                .anyMatch(row -> row.getMedicine().getId().equals(medicine.getId()));
    }

    @Test
    @Transactional
    void uniqueConstraint_rejectsDuplicatePharmacyMedicinePair() {
        Pharmacy pharmacy = pharmacyRepository.save(testPharmacy("REPO03"));
        Medicine medicine = medicineRepository.save(testMedicine("REPO-SKU-3"));

        Inventory first = new Inventory();
        first.setPharmacy(pharmacy);
        first.setMedicine(medicine);
        inventoryRepository.saveAndFlush(first);

        Inventory duplicate = new Inventory();
        duplicate.setPharmacy(pharmacy);
        duplicate.setMedicine(medicine);

        org.junit.jupiter.api.Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> inventoryRepository.saveAndFlush(duplicate));
    }

    private Pharmacy testPharmacy(String code) {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setCode(code);
        pharmacy.setName("Test Pharmacy " + code);
        pharmacy.setAddressLine1("1 Test Street");
        pharmacy.setCity("Testville");
        pharmacy.setCountry("Nigeria");
        return pharmacy;
    }

    private Medicine testMedicine(String sku) {
        Medicine medicine = new Medicine();
        medicine.setSku(sku);
        medicine.setName("Test Medicine " + sku);
        medicine.setForm(MedicineForm.TABLET);
        medicine.setUnitPrice(new BigDecimal("1.50"));
        return medicine;
    }
}
