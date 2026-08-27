package com.pharmasync.web.dto;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {

    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
