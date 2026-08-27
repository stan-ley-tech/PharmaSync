package com.pharmasync.web.dto;

import com.pharmasync.domain.catalog.Medicine;
import com.pharmasync.domain.catalog.MedicineForm;
import java.io.Serializable;
import java.math.BigDecimal;

public record MedicineResponse(
        Long id,
        String sku,
        String name,
        String genericName,
        MedicineForm form,
        String strength,
        String manufacturer,
        String unitOfMeasure,
        boolean requiresPrescription,
        boolean controlledSubstance,
        int reorderThreshold,
        int reorderQuantity,
        Long defaultSupplierId,
        BigDecimal unitPrice,
        boolean active) implements Serializable {

    public static MedicineResponse from(Medicine medicine) {
        return new MedicineResponse(
                medicine.getId(),
                medicine.getSku(),
                medicine.getName(),
                medicine.getGenericName(),
                medicine.getForm(),
                medicine.getStrength(),
                medicine.getManufacturer(),
                medicine.getUnitOfMeasure(),
                medicine.isRequiresPrescription(),
                medicine.isControlledSubstance(),
                medicine.getReorderThreshold(),
                medicine.getReorderQuantity(),
                medicine.getDefaultSupplier() != null ? medicine.getDefaultSupplier().getId() : null,
                medicine.getUnitPrice(),
                medicine.isActive());
    }
}
