package com.pharmasync.web.dto;

import com.pharmasync.domain.user.Role;
import com.pharmasync.domain.user.User;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        Long pharmacyId,
        Set<String> roles,
        boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPharmacy() != null ? user.getPharmacy().getId() : null,
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.isActive());
    }
}
