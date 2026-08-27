package com.pharmasync.web.dto;

import com.pharmasync.domain.pharmacy.Pharmacy;

public record PharmacyResponse(
        Long id,
        String code,
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String phone,
        boolean active) {

    public static PharmacyResponse from(Pharmacy pharmacy) {
        return new PharmacyResponse(
                pharmacy.getId(),
                pharmacy.getCode(),
                pharmacy.getName(),
                pharmacy.getAddressLine1(),
                pharmacy.getAddressLine2(),
                pharmacy.getCity(),
                pharmacy.getState(),
                pharmacy.getPostalCode(),
                pharmacy.getCountry(),
                pharmacy.getPhone(),
                pharmacy.isActive());
    }
}
