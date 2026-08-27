package com.pharmasync.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmasync.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void login_withValidCredentials_returnsTokenPair() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "ChangeMe123!"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("accessToken")).isNotNull();
        assertThat(response.getBody().get("refreshToken")).isNotNull();
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "not-the-password"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withoutToken_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/medicines/1", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminOnlyEndpoint_withNonAdminToken_returnsForbidden() {
        Map<String, Object> createUser = Map.of(
                "username", "auth-test-doctor",
                "email", "auth-test-doctor@pharmasync.local",
                "password", "Password123!",
                "firstName", "Auth",
                "lastName", "Doctor",
                "roles", java.util.List.of("DOCTOR"));

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        adminHeaders.setBearerAuth(login("admin", "ChangeMe123!"));
        restTemplate.exchange("/api/users", HttpMethod.POST, new HttpEntity<>(createUser, adminHeaders), Map.class);

        String doctorToken = login("auth-test-doctor", "Password123!");
        HttpHeaders doctorHeaders = new HttpHeaders();
        doctorHeaders.setBearerAuth(doctorToken);

        ResponseEntity<Map> response = restTemplate.exchange("/api/users/1", HttpMethod.GET,
                new HttpEntity<>(doctorHeaders), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void refresh_withValidRefreshToken_issuesNewAccessToken() {
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "ChangeMe123!"), Map.class);
        String refreshToken = (String) loginResponse.getBody().get("refreshToken");

        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity("/api/auth/refresh",
                Map.of("refreshToken", refreshToken), Map.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody().get("accessToken")).isNotNull();
    }

    private String login(String username, String password) {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login",
                Map.of("username", username, "password", password), Map.class);
        return (String) response.getBody().get("accessToken");
    }
}
