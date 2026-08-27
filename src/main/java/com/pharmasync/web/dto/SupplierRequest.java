package com.pharmasync.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank String code,
        @NotBlank String name,
        String contactName,
        @Email String email,
        String phone,
        String apiBaseUrl) {
}
