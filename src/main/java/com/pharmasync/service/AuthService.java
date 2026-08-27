package com.pharmasync.service;

import com.pharmasync.web.dto.LoginRequest;
import com.pharmasync.web.dto.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);
}
