package com.qvety.platform;

import static com.qvety.ApiTestSupport.ADMIN;
import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.PASSWORD;
import static com.qvety.ApiTestSupport.PRACTICE_ID;
import static com.qvety.ApiTestSupport.client;
import static com.qvety.ApiTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.qvety.TestcontainersConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * Part 11. The super admin side, and the wall between it and a clinic.
 *
 * Status is exercised on a practice this test creates rather than the seeded one, so suspending and
 * closing cannot strand the other tests. The status filter caches for a minute, so each case uses a
 * fresh practice instead of moving one back and forth.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class PlatformIT {

    static final String STAFF = "staff@qvety.example.com";
    static final String PRACTICES = "/api/platform/practices";

    @LocalServerPort
    int port;

    RestClient api() {
        return client(port);
    }

    @SuppressWarnings("unchecked")
    private String staffToken() {
        var body = api().post().uri("/api/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", STAFF, "password", PASSWORD)).retrieve().body(Map.class);
        return (String) body.get("token");
    }

    private static Map<String, Object> newPractice(String name, String phone) {
        var body = new HashMap<String, Object>();
        body.put("name", name);
        body.put("country", "EG");
        body.put("currency", "EGP");
        body.put("locale", "ar-EG");
        body.put("timezone", "Africa/Cairo");
        body.put("adminName", "Owner of " + name);
        body.put("adminPhone", phone);
        body.put("adminVeterinarian", true);
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> create(String token, String name, String phone) {
        var response = api().post().uri(PRACTICES).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).body(newPractice(name, phone))
            .retrieve().toEntity(Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private void setStatus(String token, String practiceId, String status, String reason) {
        var response = api().post().uri(PRACTICES + "/" + practiceId + "/status")
            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("status", status, "reason", reason)).retrieve().toEntity(Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // the answer must carry the new status, not the one the row had a moment ago: the screen
        // renders straight from this body and would otherwise show the move as having done nothing
        assertThat(response.getBody()).containsEntry("status", status);
    }

    // ---- who may go where --------------------------------------------------------------------------

    @Test
    void platformLoginWorksAndWrongPasswordDoesNot() {
        assertThat(staffToken()).isNotBlank();

        var wrong = api().post().uri("/api/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", STAFF, "password", "not-the-password")).retrieve().toEntity(String.class);
        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var unknown = api().post().uri("/api/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", "nobody@qvety.example.com", "password", PASSWORD)).retrieve().toEntity(String.class);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void theTwoSidesCannotEnterEachOther() {
        var staff = staffToken();
        var practiceAdmin = login(api(), ADMIN, PASSWORD);

        // the super admin sees every practice, from system context
        List<Map<String, Object>> practices = api().get().uri(PRACTICES)
            .header("Authorization", "Bearer " + staff).retrieve().body(List.class);
        assertThat(practices).isNotEmpty()
            .anySatisfy(p -> assertThat(p).containsEntry("id", PRACTICE_ID));

        // but carries no practice, so a clinic endpoint refuses rather than failing on a missing tenant
        var staffOnTenant = api().get().uri("/api/v1/clients").header("Authorization", "Bearer " + staff)
            .retrieve().toEntity(String.class);
        assertThat(staffOnTenant.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // and a clinic admin is nobody on the platform side
        var adminOnPlatform = api().get().uri(PRACTICES).header("Authorization", "Bearer " + practiceAdmin)
            .retrieve().toEntity(String.class);
        assertThat(adminOnPlatform.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- creation ----------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void creatingAPracticeBuildsEverythingItNeeds() {
        var staff = staffToken();
        var created = create(staff, "Delta Vet", "01100000001");

        var practice = (Map<String, Object>) created.get("practice");
        assertThat(practice).containsEntry("status", "trial").containsEntry("country", "EG");
        assertThat(practice.get("trialEndsAt")).isNotNull();
        assertThat((String) created.get("adminPhone")).isEqualTo("+201100000001");   // normalized to E.164
        var temporary = (String) created.get("temporaryPassword");
        assertThat(temporary).isNotBlank();

        // the new owner can log in, and is made to change the password before anything else
        var ownerToken = login(api(), "+201100000001", temporary);
        var gated = api().get().uri("/api/v1/clients").header("Authorization", "Bearer " + ownerToken)
            .retrieve().toEntity(String.class);
        assertThat(gated.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(gated.getBody()).contains("password_change_required");

        // the country's starter catalogue came with it
        var me = api().get().uri("/api/v1/me").header("Authorization", "Bearer " + ownerToken).retrieve().body(Map.class);
        assertThat(me).containsEntry("role", "admin");
    }

    @Test
    @SuppressWarnings("unchecked")
    void aCountryWithNoCatalogueWritesNothing() {
        var staff = staffToken();
        List<Map<String, Object>> before = api().get().uri(PRACTICES)
            .header("Authorization", "Bearer " + staff).retrieve().body(List.class);

        var body = newPractice("Nowhere Vet", "01100000002");
        body.put("country", "ZZ");
        var refused = api().post().uri(PRACTICES).header("Authorization", "Bearer " + staff)
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(Map.class);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(refused.getBody()).containsEntry("code", "practice.country_unsupported");

        List<Map<String, Object>> after = api().get().uri(PRACTICES)
            .header("Authorization", "Bearer " + staff).retrieve().body(List.class);
        assertThat(after).hasSameSizeAs(before);
    }

    // ---- status enforcement -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void suspendedReadsButDoesNotWrite() {
        var staff = staffToken();
        var created = create(staff, "Suspended Vet", "01100000003");
        var practiceId = (String) ((Map<String, Object>) created.get("practice")).get("id");
        var owner = firstLogin(created);

        setStatus(staff, practiceId, "suspended", "did not pay for two months");

        var read = api().get().uri("/api/v1/clients").header("Authorization", "Bearer " + owner)
            .retrieve().toEntity(String.class);
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);

        var write = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + owner)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "New Client", "phone", "+201111111111"))
            .retrieve().toEntity(Map.class);
        assertThat(write.getStatusCode()).isEqualTo(HttpStatus.valueOf(423));
        assertThat(write.getBody()).containsEntry("code", "platform.suspended");
    }

    @Test
    @SuppressWarnings("unchecked")
    void closedAllowsOnlyTheExport() {
        var staff = staffToken();
        var created = create(staff, "Closed Vet", "01100000004");
        var practiceId = (String) ((Map<String, Object>) created.get("practice")).get("id");
        var owner = firstLogin(created);

        setStatus(staff, practiceId, "closed", "the owner retired");

        var anything = api().get().uri("/api/v1/clients").header("Authorization", "Bearer " + owner)
            .retrieve().toEntity(Map.class);
        assertThat(anything.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(anything.getBody()).containsEntry("code", "platform.closed");

        // the clinic can still take its data with it
        var export = api().get().uri("/api/v1/practice/export").header("Authorization", "Bearer " + owner)
            .retrieve().toEntity(String.class);
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);

        // and closed_at was recorded
        var row = api().get().uri(PRACTICES + "/" + practiceId).header("Authorization", "Bearer " + staff)
            .retrieve().body(Map.class);
        assertThat(row).containsEntry("status", "closed");
        assertThat(row.get("closedAt")).isNotNull();
    }

    @Test
    void aStatusChangeNeedsAReason() {
        var staff = staffToken();
        var created = create(staff, "Reasonless Vet", "01100000005");
        @SuppressWarnings("unchecked")
        var practiceId = (String) ((Map<String, Object>) created.get("practice")).get("id");

        var refused = api().post().uri(PRACTICES + "/" + practiceId + "/status")
            .header("Authorization", "Bearer " + staff).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("status", "suspended", "reason", "  ")).retrieve().toEntity(String.class);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- export -------------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void anExportHoldsOnlyItsOwnPractice() {
        var staff = staffToken();
        var seededAdmin = login(api(), ADMIN, PASSWORD);

        var own = api().get().uri("/api/v1/practice/export").header("Authorization", "Bearer " + seededAdmin)
            .retrieve().body(String.class);
        assertThat(own).contains("\"exported_at\"").contains("\"tables\"").contains("\"attachments\"");
        assertThat(own).contains(PRACTICE_ID);

        // a second practice exists from the other tests; none of its rows may appear
        var other = create(staff, "Other Export Vet", "01100000006");
        var otherId = (String) ((Map<String, Object>) other.get("practice")).get("id");
        assertThat(own).doesNotContain(otherId);

        // the same document, taken by the super admin, is recorded as an access
        var byStaff = api().get().uri(PRACTICES + "/" + PRACTICE_ID + "/export")
            .header("Authorization", "Bearer " + staff).retrieve().toEntity(String.class);
        assertThat(byStaff.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byStaff.getBody()).contains(PRACTICE_ID);
    }

    @Test
    void onlyAnAdminTakesTheClinicsExport() {
        var desk = login(api(), DESK, PASSWORD);
        var refused = api().get().uri("/api/v1/practice/export").header("Authorization", "Bearer " + desk)
            .retrieve().toEntity(String.class);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /** A brand-new owner must change the password before anything else, so the test does that first. */
    @SuppressWarnings("unchecked")
    private String firstLogin(Map<String, Object> created) {
        var phone = (String) created.get("adminPhone");
        var temporary = (String) created.get("temporaryPassword");
        var token = login(api(), phone, temporary);
        var changed = api().post().uri("/api/v1/me/password").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("currentPassword", temporary, "newPassword", "a-real-password-now"))
            .retrieve().body(Map.class);
        return (String) changed.get("token");
    }
}
