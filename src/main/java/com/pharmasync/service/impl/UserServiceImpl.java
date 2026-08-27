package com.pharmasync.service.impl;

import com.pharmasync.domain.pharmacy.Pharmacy;
import com.pharmasync.domain.user.Role;
import com.pharmasync.domain.user.User;
import com.pharmasync.exception.DuplicateResourceException;
import com.pharmasync.exception.ResourceNotFoundException;
import com.pharmasync.repository.PharmacyRepository;
import com.pharmasync.repository.RoleRepository;
import com.pharmasync.repository.UserRepository;
import com.pharmasync.service.UserService;
import com.pharmasync.web.dto.CreateUserRequest;
import com.pharmasync.web.dto.UserResponse;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username " + request.username() + " is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email " + request.email() + " is already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setLicenseNumber(request.licenseNumber());
        user.setRoles(resolveRoles(request.roles()));

        if (request.pharmacyId() != null) {
            Pharmacy pharmacy = pharmacyRepository.findById(request.pharmacyId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Pharmacy", request.pharmacyId()));
            user.setPharmacy(pharmacy);
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            roles.add(roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + roleName)));
        }
        return roles;
    }
}
