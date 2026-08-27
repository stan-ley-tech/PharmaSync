package com.pharmasync.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PharmacyRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String addressLine1,
        String addressLine2,
        @NotBlank String city,
        String state,
        String postalCode,
        @NotBlank String country,
        String phone) {
}
