package com.pharmasync.service.impl;

import com.pharmasync.config.JwtProperties;
import com.pharmasync.security.CustomUserDetailsService;
import com.pharmasync.security.JwtService;
import com.pharmasync.security.SecurityUser;
import com.pharmasync.service.AuthService;
import com.pharmasync.web.dto.LoginRequest;
import com.pharmasync.web.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Override
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityUser user = (SecurityUser) userDetailsService.loadUserByUsername(request.username());
        return issueTokens(user);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isRefreshToken(refreshToken) || !jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new BadCredentialsException("Refresh token is invalid or expired");
        }

        return issueTokens((SecurityUser) userDetails);
    }

    private TokenResponse issueTokens(SecurityUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return TokenResponse.of(accessToken, refreshToken, jwtProperties.accessTokenTtlMinutes() * 60);
    }
}
