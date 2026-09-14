package com.qvety.auth;

import static com.qvety.ApiTestSupport.ADMIN;
import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.DESK_PHONE;
import static com.qvety.ApiTestSupport.PASSWORD;
import static com.qvety.ApiTestSupport.TECH;
import static com.qvety.ApiTestSupport.VET;
import static com.qvety.ApiTestSupport.client;
import static com.qvety.ApiTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.qvety.TestcontainersConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** Part 03: login, roles, the veterinarian flag, revocation, forced password change, rate limiting. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class AuthIT {

    @LocalServerPort
    int port;

    RestClient api() {
        return client(port);
    }

    @Test
    void frontDeskCanReadMeButNotUsers() {
        var token = login(api(), DESK, PASSWORD);

        var me = api().get().uri("/api/v1/me").header("Authorization", "Bearer " + token)
            .retrieve().toEntity(Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("email", DESK).containsEntry("role", "front_desk");

        var users = api().get().uri("/api/v1/users").header("Authorization", "Bearer " + token)
            .retrieve().toEntity(String.class);
        assertThat(users.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void loginAcceptsPhoneInAnyShapeOrEmail() {
        assertThat(login(api(), DESK_PHONE, PASSWORD)).isNotBlank();          // +201000000104
        assertThat(login(api(), "01000000104", PASSWORD)).isNotBlank();       // local shape
        assertThat(login(api(), "0100 000 0104", PASSWORD)).isNotBlank();     // with spaces
        assertThat(login(api(), DESK, PASSWORD)).isNotBlank();                // email
        var bad = api().post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("identifier", "1000000104", "password", PASSWORD)).retrieve().toEntity(String.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);   // no trunk zero: unknown, not 400
    }

    @Test
    void wrongPasswordIs401AndNoTokenIs401() {
        var wrong = api().post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("identifier", DESK, "password", "not-the-password"))
            .retrieve().toEntity(String.class);
        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var anonymous = api().get().uri("/api/v1/me").retrieve().toEntity(String.class);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void vetFlagNotRoleDecidesClinicalAccess() {
        // admin (with the flag) and veterinarian pass; technician and front desk do not
        assertThat(probe(login(api(), ADMIN, PASSWORD))).isEqualTo(HttpStatus.OK);
        assertThat(probe(login(api(), VET, PASSWORD))).isEqualTo(HttpStatus.OK);
        assertThat(probe(login(api(), TECH, PASSWORD))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(probe(login(api(), DESK, PASSWORD))).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminResetRevokesOldTokenAndForcesPasswordChange() {
        var admin = login(api(), ADMIN, PASSWORD);
        var created = api().post().uri("/api/v1/users").header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("phone", "01000000201", "email", "reset-case@clinic.example.com", "temporaryPassword", "temporary-1",
                "fullName", "Reset Case", "role", "front_desk", "veterinarian", false))
            .retrieve().toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = (String) created.getBody().get("id");

        // new user must change the password first: /me works, /users is 403 password_change_required
        var first = login(api(), "reset-case@clinic.example.com", "temporary-1");
        assertThat(status("/api/v1/me", first)).isEqualTo(HttpStatus.OK);
        var gated = api().get().uri("/api/v1/practice").header("Authorization", "Bearer " + first)
            .retrieve().toEntity(String.class);
        assertThat(gated.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(gated.getBody()).contains("password_change_required");

        var changed = api().post().uri("/api/v1/me/password").header("Authorization", "Bearer " + first)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("currentPassword", "temporary-1", "newPassword", "brand-new-password"))
            .retrieve().toEntity(Map.class);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        var fresh = (String) changed.getBody().get("token");
        assertThat(status("/api/v1/me", first)).isEqualTo(HttpStatus.UNAUTHORIZED);   // old token dead
        assertThat(status("/api/v1/practice", fresh)).isEqualTo(HttpStatus.OK);

        // admin resets: the fresh token dies; next login is gated again
        var reset = api().post().uri("/api/v1/users/" + id + "/reset-password").header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("temporaryPassword", "temporary-2"))
            .retrieve().toEntity(String.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(status("/api/v1/me", fresh)).isEqualTo(HttpStatus.UNAUTHORIZED);
        var again = login(api(), "reset-case@clinic.example.com", "temporary-2");
        assertThat(status("/api/v1/practice", again)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deactivatedUserTokenIs401() {
        var admin = login(api(), ADMIN, PASSWORD);
        var created = api().post().uri("/api/v1/users").header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("phone", "01000000202", "email", "deactivate-case@clinic.example.com", "temporaryPassword", "temporary-1",
                "fullName", "Deactivate Case", "role", "technician", "veterinarian", false))
            .retrieve().toEntity(Map.class);
        var id = (String) created.getBody().get("id");
        var token = login(api(), "deactivate-case@clinic.example.com", "temporary-1");
        assertThat(status("/api/v1/me", token)).isEqualTo(HttpStatus.OK);

        api().post().uri("/api/v1/users/" + id + "/deactivate").header("Authorization", "Bearer " + admin)
            .retrieve().toEntity(String.class);

        assertThat(status("/api/v1/me", token)).isEqualTo(HttpStatus.UNAUTHORIZED);
        var relogin = api().post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("identifier", "deactivate-case@clinic.example.com", "password", "temporary-1"))
            .retrieve().toEntity(String.class);
        assertThat(relogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void eleventhFailedLoginIs429WithRetryAfter() {
        var email = "ratelimit-case@clinic.example.com";   // unknown user: failures still count
        for (int i = 0; i < 10; i++) {
            var r = api().post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("identifier", email, "password", "wrong")).retrieve().toEntity(String.class);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        var eleventh = api().post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("identifier", email, "password", "wrong")).retrieve().toEntity(String.class);
        assertThat(eleventh.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(eleventh.getHeaders().getFirst("Retry-After")).isNotBlank();
    }

    @Test
    void adminEditsPracticeIdentityFrontDeskCannot() {
        var admin = login(api(), ADMIN, PASSWORD);
        var body = Map.of("name", "Neighborhood Vet", "address", "5 New St, Cairo", "phone", "+201000000009",
            "taxRatePercent", 0);
        var ok = api().put().uri("/api/v1/practice").header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(Map.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody()).containsEntry("address", "5 New St, Cairo").containsEntry("status", "trial");

        var desk = login(api(), DESK, PASSWORD);
        var forbidden = api().put().uri("/api/v1/practice").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void securityHeadersArePresent() {
        var token = login(api(), DESK, PASSWORD);
        var r = api().get().uri("/api/v1/me").header("Authorization", "Bearer " + token).retrieve().toEntity(String.class);
        assertThat(r.getHeaders().getFirst("Content-Security-Policy")).contains("default-src 'self'").contains("frame-ancestors 'none'");
        assertThat(r.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(r.getHeaders().getFirst("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
    }

    private HttpStatus probe(String token) {
        return status("/api/v1/test/vet-only", token);
    }

    private HttpStatus status(String path, String token) {
        var r = api().get().uri(path).header("Authorization", "Bearer " + token).retrieve().toEntity(String.class);
        return HttpStatus.valueOf(r.getStatusCode().value());
    }
}
