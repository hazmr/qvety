package com.qvety.reference;

import static com.qvety.ApiTestSupport.ADMIN;
import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.PASSWORD;
import static com.qvety.ApiTestSupport.client;
import static com.qvety.ApiTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.qvety.TestcontainersConfig;
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
 * Part 09. Rooms are exercised end to end because the generic service is the same code behind all three;
 * appointment types and services are checked only where they differ (their own columns, the server-set
 * currency). The cross-tenant case lives in TenantIsolationIT beside the other tables.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class ReferenceIT {

    static final String ROOMS = "/api/v1/reference/rooms";
    static final String TYPES = "/api/v1/reference/appointment-types";
    static final String SERVICES = "/api/v1/reference/services";
    /** Seeded inactive by R__050_dev_catalog.sql, so the default list must not show it. */
    static final String INACTIVE_ROOM = "00000000-0000-7000-8000-000000000604";

    @LocalServerPort
    int port;

    RestClient api() {
        return client(port);
    }

    @Test
    @SuppressWarnings("unchecked")
    void roomLivesItsWholeLife() {
        var admin = login(api(), ADMIN, PASSWORD);

        // create
        var created = api().post().uri(ROOMS).header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("name", "غرفة الأشعة"))
            .retrieve().toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).containsEntry("name", "غرفة الأشعة").containsEntry("active", true);
        var id = (String) created.getBody().get("id");

        // a second row with the same name, in any case, is refused
        var duplicate = api().post().uri(ROOMS).header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("name", "غرفة الأشعة"))
            .retrieve().toEntity(Map.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody()).containsEntry("code", "reference.name_taken");

        // update with the version the caller holds; a stale one is 409
        var updated = api().put().uri(ROOMS + "/" + id).header("Authorization", "Bearer " + admin)
            .header("If-Match", "0").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "غرفة الأشعة والسونار")).retrieve().toEntity(Map.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("name", "غرفة الأشعة والسونار");

        var stale = api().put().uri(ROOMS + "/" + id).header("Authorization", "Bearer " + admin)
            .header("If-Match", "0").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "أي اسم")).retrieve().toEntity(Map.class);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(stale.getBody()).containsEntry("code", "stale_update");

        // deactivate: gone from the default list, still there with the flag, and pressing it twice is fine
        assertThat(deactivate(admin, ROOMS, id).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(names(admin, ROOMS, false)).doesNotContain("غرفة الأشعة والسونار");
        assertThat(names(admin, ROOMS, true)).contains("غرفة الأشعة والسونار");
        assertThat(deactivate(admin, ROOMS, id).getBody()).containsEntry("active", false);

        // activate: back in the pickers, and pressing it twice is fine
        assertThat(activate(admin, ROOMS, id).getBody()).containsEntry("active", true);
        assertThat(activate(admin, ROOMS, id).getBody()).containsEntry("active", true);
        assertThat(names(admin, ROOMS, false)).contains("غرفة الأشعة والسونار");

        // leave the practice as the seed left it
        deactivate(admin, ROOMS, id);
    }

    @Test
    void inactiveRowsAreHiddenUnlessAskedFor() {
        var desk = login(api(), DESK, PASSWORD);   // everyone reads
        var visible = names(desk, ROOMS, false);
        var all = names(desk, ROOMS, true);
        assertThat(visible).doesNotContain("غرفة الحجز");
        assertThat(all).contains("غرفة الحجز");
        // live rows first, so the inactive ones never interleave
        assertThat(all.indexOf("غرفة الحجز")).isEqualTo(all.size() - 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void serviceCurrencyComesFromThePracticeNotTheRequest() {
        var admin = login(api(), ADMIN, PASSWORD);
        var created = api().post().uri(SERVICES).header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "تنظيف أسنان", "price", "450.00", "currency", "USD"))   // currency ignored
            .retrieve().toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).containsEntry("currency", "EGP").containsEntry("price", 450.00);
        deactivate(admin, SERVICES, (String) created.getBody().get("id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void appointmentTypeChecksDurationAndColour() {
        var admin = login(api(), ADMIN, PASSWORD);

        var tooLong = api().post().uri(TYPES).header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "يوم كامل", "durationMinutes", 600)).retrieve().toEntity(Map.class);
        assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) tooLong.getBody().get("fields")).containsKey("durationMinutes");

        var badColour = api().post().uri(TYPES).header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "متابعة", "durationMinutes", 20, "color", "teal")).retrieve().toEntity(Map.class);
        assertThat(badColour.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) badColour.getBody().get("fields")).containsKey("color");

        var ok = api().post().uri(TYPES).header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "متابعة", "durationMinutes", 20, "color", "#4A6FA5")).retrieve().toEntity(Map.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        deactivate(admin, TYPES, (String) ok.getBody().get("id"));
    }

    @Test
    void onlyAdminWrites() {
        var desk = login(api(), DESK, PASSWORD);
        assertThat(api().get().uri(ROOMS).header("Authorization", "Bearer " + desk)
            .retrieve().toEntity(String.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        var create = api().post().uri(ROOMS).header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("name", "غرفة مهربة"))
            .retrieve().toEntity(Map.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(create.getBody()).containsEntry("code", "forbidden");

        var deactivate = api().post().uri(ROOMS + "/" + INACTIVE_ROOM + "/activate")
            .header("Authorization", "Bearer " + desk).retrieve().toEntity(String.class);
        assertThat(deactivate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unknownIdIsNotFound() {
        var admin = login(api(), ADMIN, PASSWORD);
        var missing = api().get().uri(ROOMS + "/00000000-0000-7000-8000-0000000009ff")
            .header("Authorization", "Bearer " + admin).retrieve().toEntity(Map.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).containsEntry("code", "reference.not_found");
    }

    @SuppressWarnings("unchecked")
    private List<String> names(String token, String path, boolean includeInactive) {
        List<Map<String, Object>> rows = api().get().uri(path + "?includeInactive=" + includeInactive)
            .header("Authorization", "Bearer " + token).retrieve().body(List.class);
        return rows.stream().map(r -> (String) r.get("name")).toList();
    }

    private org.springframework.http.ResponseEntity<Map> deactivate(String token, String path, String id) {
        return api().post().uri(path + "/" + id + "/deactivate").header("Authorization", "Bearer " + token)
            .retrieve().toEntity(Map.class);
    }

    private org.springframework.http.ResponseEntity<Map> activate(String token, String path, String id) {
        return api().post().uri(path + "/" + id + "/activate").header("Authorization", "Bearer " + token)
            .retrieve().toEntity(Map.class);
    }
}
