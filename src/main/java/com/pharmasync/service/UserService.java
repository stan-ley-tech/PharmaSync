package com.pharmasync.service;

import com.pharmasync.web.dto.CreateUserRequest;
import com.pharmasync.web.dto.UserResponse;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getById(Long id);
}
