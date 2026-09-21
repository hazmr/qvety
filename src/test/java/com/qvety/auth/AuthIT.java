package com.qvety.auth;

import static com.qvety.ApiTestSupport.ADMIN;
import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.DESK_PHONE;
import static com.qvety.ApiTestSupport.PASSWORD;
import static com.qvety.ApiTestSupport.PRACTICE_ID;
import static com.qvety.ApiTestSupport.TECH;
import static com.qvety.ApiTestSupport.VET;
import static com.qvety.ApiTestSupport.client;
import static com.qvety.ApiTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.qvety.TestcontainersConfig;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.web.client.RestClient;

/**
 * Part 03: login, roles, the veterinarian flag, revocation, forced password change, rate limiting.
 * Part 03b: the same phone at several practices. Practices C and D each get a front-desk user with the
 * seed desk phone; C with its own password, D with the seed password, so both branches are covered.
 * D starts inactive so the part 03 tests keep a single match; the 03b tests activate it and put it back.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class AuthIT {

    static final String PRACTICE_C = "00000000-0000-7000-8000-000000000003";
    static final String PRACTICE_D = "00000000-0000-7000-8000-000000000004";
    static final String USER_C = "00000000-0000-7000-8000-000000000301";
    static final String USER_D = "00000000-0000-7000-8000-000000000401";
    static final String PASSWORD_C = "otherclinic99";

    @LocalServerPort
    int port;
    @Autowired PostgreSQLContainer postgres;
    @Autowired LoginRateLimiter limiter;

    RestClient api() {
        return client(port);
    }

    @BeforeAll
    static void seedSamePhoneAtTwoMorePractices(@Autowired PostgreSQLContainer postgres, @Autowired PasswordEncoder encoder) throws SQLException {
        try (var owner = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var st = owner.createStatement()) {
            st.execute("""
                INSERT INTO practices (id, name, country, currency, locale, timezone) VALUES
                  ('%s', 'Third Clinic', 'EG', 'EGP', 'ar-EG', 'Africa/Cairo'),
                  ('%s', 'Fourth Clinic', 'EG', 'EGP', 'ar-EG', 'Africa/Cairo')
                ON CONFLICT (id) DO NOTHING
                """.formatted(PRACTICE_C, PRACTICE_D));
            st.execute("""
                INSERT INTO users (id, practice_id, phone, password_hash, full_name, role, is_veterinarian, active) VALUES
                  ('%s', '%s', '%s', '%s', 'Desk At C', 'front_desk', false, true),
                  ('%s', '%s', '%s', '%s', 'Desk At D', 'front_desk', false, false)
                ON CONFLICT (id) DO NOTHING
                """.formatted(USER_C, PRACTICE_C, DESK_PHONE, encoder.encode(PASSWORD_C),
                               USER_D, PRACTICE_D, DESK_PHONE, encoder.encode(PASSWORD)));
        }
    }

    @Test
    void frontDeskCanReadMeButNotUsers() {
        var token = login(api(), DESK, PASSWORD);

        var me = api().get().uri("/api/v1/me").header("Authorization", "Bearer " + token)
            .retrieve().toEntity(Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("email", DESK).containsEntry("role", "front_desk");

        var users = api().get().uri("/api/v1/users").header("Authorization", "Bearer " + token)
            .retrieve().toEntity(Map.class);
        assertThat(users.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(users.getBody()).containsEntry("code", "forbidden").containsKey("message");   // an ApiError, not an empty body

        var arabic = api().get().uri("/api/v1/users").header("Authorization", "Bearer " + token)
            .header("Accept-Language", "ar-EG").retrieve().toEntity(Map.class);
        assertThat(arabic.getBody()).containsEntry("message", "غير مسموح لك بهذا الإجراء.");
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
        var gatedArabic = api().get().uri("/api/v1/practice").header("Authorization", "Bearer " + first)
            .header("Accept-Language", "ar-EG").retrieve().toEntity(Map.class);
        assertThat(gatedArabic.getBody()).containsEntry("code", "password_change_required")
            .containsEntry("message", "يجب تغيير كلمة المرور قبل المتابعة.");   // the gate answers in the caller's language

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

        // Returning staff member: same row comes back with a fresh temporary password and a forced change.
        api().post().uri("/api/v1/users/" + id + "/activate").header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("temporaryPassword", "temporary-2"))
            .retrieve().toEntity(String.class);
        var back = login(api(), "deactivate-case@clinic.example.com", "temporary-2");
        assertThat(status("/api/v1/me", back)).isEqualTo(HttpStatus.OK);
        assertThat(status("/api/v1/practice", back)).isEqualTo(HttpStatus.FORBIDDEN);   // must change password first
        var twice = api().post().uri("/api/v1/users/" + id + "/activate").header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("temporaryPassword", "temporary-3"))
            .retrieve().toEntity(String.class);
        assertThat(twice.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void logoutRevokesTheTokenOnTheServer() {
        var token = login(api(), TECH, PASSWORD);
        assertThat(status("/api/v1/me", token)).isEqualTo(HttpStatus.OK);

        var out = api().post().uri("/api/v1/auth/logout").header("Authorization", "Bearer " + token)
            .retrieve().toEntity(String.class);
        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(status("/api/v1/me", token)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(status("/api/v1/me", login(api(), TECH, PASSWORD))).isEqualTo(HttpStatus.OK);   // a fresh login works
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

    // ---- part 03b: the same phone at several practices ----------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void samePhoneAtTwoPractices() throws SQLException {
        // different password: the match names the practice, no picker
        var c = attempt(DESK_PHONE, PASSWORD_C, null);
        assertThat(c.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(c.getBody()).containsKey("token").doesNotContainKey("practices");
        assertThat(practiceName((String) c.getBody().get("token"))).isEqualTo("Third Clinic");

        // D inactive: only A matches the shared password, so a token without a picker
        var onlyA = attempt(DESK_PHONE, PASSWORD, null);
        assertThat(onlyA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(onlyA.getBody()).containsKey("token").doesNotContainKey("practices");

        setActive(USER_D, true);
        try {
            // same password at A and D: the practice list, no token
            var pick = attempt(DESK_PHONE, PASSWORD, null);
            assertThat(pick.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(pick.getBody()).doesNotContainKey("token").doesNotContainKey("user");
            var practices = (List<Map<String, Object>>) pick.getBody().get("practices");
            assertThat(practices).extracting(p -> p.get("id")).containsExactlyInAnyOrder(PRACTICE_ID, PRACTICE_D);
            assertThat(practices).extracting(p -> p.get("name")).contains("Fourth Clinic");

            // chosen practice: a token for that practice only
            var chosen = attempt(DESK_PHONE, PASSWORD, PRACTICE_D);
            assertThat(chosen.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(practiceName((String) chosen.getBody().get("token"))).isEqualTo("Fourth Clinic");

            // practiceId never widens: C's row does not match this password, B has no such user
            assertThat(attempt(DESK_PHONE, PASSWORD, PRACTICE_C).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(attempt(DESK_PHONE, PASSWORD, "00000000-0000-7000-8000-000000000002").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

            // wrong password everywhere: 401 and no list
            var wrong = attempt(DESK_PHONE, "wrong-everywhere", null);
            assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(wrong.getBody()).doesNotContainKey("practices");
        } finally {
            setActive(USER_D, false);
        }
    }

    @Test
    void practiceListResetsTheIdentifierBucket() throws SQLException {
        var ip = "10.0.0.1";   // a fake address: the shared 127.0.0.1 bucket stays untouched
        for (int i = 0; i < 9; i++) limiter.recordFailure(DESK_PHONE, ip);
        assertThat(limiter.retryAfterSeconds(DESK_PHONE, ip)).isZero();

        setActive(USER_D, true);
        try {
            var pick = attempt(DESK_PHONE, PASSWORD, null);
            assertThat(pick.getBody()).containsKey("practices");   // correct password, choice pending
        } finally {
            setActive(USER_D, false);
        }

        for (int i = 0; i < 9; i++) limiter.recordFailure(DESK_PHONE, ip);
        assertThat(limiter.retryAfterSeconds(DESK_PHONE, ip)).isZero();   // 18 failures would be locked without the reset
        limiter.recordSuccess(DESK_PHONE);
    }

    @SuppressWarnings("unchecked")
    private org.springframework.http.ResponseEntity<Map> attempt(String identifier, String password, String practiceId) {
        Map<String, Object> body = new java.util.HashMap<>(Map.of("identifier", identifier, "password", password));
        if (practiceId != null) body.put("practiceId", practiceId);
        return api().post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(body).retrieve().toEntity(Map.class);
    }

    private String practiceName(String token) {
        var r = api().get().uri("/api/v1/practice").header("Authorization", "Bearer " + token).retrieve().toEntity(Map.class);
        return (String) r.getBody().get("name");
    }

    private void setActive(String userId, boolean active) throws SQLException {
        try (var owner = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var st = owner.createStatement()) {
            st.execute("UPDATE users SET active = %s WHERE id = '%s'".formatted(active, userId));
        }
    }

    private HttpStatus probe(String token) {
        return status("/api/v1/test/vet-only", token);
    }

    private HttpStatus status(String path, String token) {
        var r = api().get().uri(path).header("Authorization", "Bearer " + token).retrieve().toEntity(String.class);
        return HttpStatus.valueOf(r.getStatusCode().value());
    }
}
