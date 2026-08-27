package com.pharmasync.web.dto;

import com.pharmasync.domain.catalog.Supplier;

public record SupplierResponse(
        Long id,
        String code,
        String name,
        String contactName,
        String email,
        String phone,
        String apiBaseUrl,
        boolean active) {

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getApiBaseUrl(),
                supplier.isActive());
    }
}
