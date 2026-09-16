package com.qvety.patients;

import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.PASSWORD;
import static com.qvety.ApiTestSupport.PRACTICE_ID;
import static com.qvety.ApiTestSupport.VET;
import static com.qvety.ApiTestSupport.client;
import static com.qvety.ApiTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qvety.TestcontainersConfig;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * Part 08: patients under a client, filters, transfer keeps history, deceased, weights, and allergies that
 * the database lets nobody edit or delete. Cross-tenant cases live in TenantIsolationIT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class PatientIT {

    static final String AHMED = "00000000-0000-7000-8000-000000000301";
    static final String FATMA = "00000000-0000-7000-8000-000000000302";
    static final String SARAH = "00000000-0000-7000-8000-000000000303";
    static final String REX = "00000000-0000-7000-8000-000000000402";              // Ahmed's dog
    static final String REX_PENICILLIN = "00000000-0000-7000-8000-000000000601";   // seeded allergy on Rex

    @LocalServerPort int port;
    @Autowired DataSource appDataSource;

    RestClient api() {
        return client(port);
    }

    // ---- 3.1 list by client; filter by species ---------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void listsByClientAndFiltersBySpecies() {
        var desk = login(api(), DESK, PASSWORD);

        var byClient = api().get().uri("/api/v1/patients?clientId=" + AHMED).header("Authorization", "Bearer " + desk)
            .retrieve().body(Map.class);
        var rows = (List<Map<String, Object>>) byClient.get("content");
        assertThat(rows).extracting(p -> p.get("clientId")).containsOnly(AHMED);
        assertThat(rows).extracting(p -> p.get("name")).contains("بسبس", "ريكس");

        var cats = api().get().uri("/api/v1/patients?clientId=" + AHMED + "&species=cat").header("Authorization", "Bearer " + desk)
            .retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) cats.get("content")).extracting(p -> p.get("species")).containsOnly("cat");

        // living only by default when asked; the seeded deceased cat (Simba) is excluded
        var living = api().get().uri("/api/v1/patients?deceased=false").header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) living.get("content")).extracting(p -> p.get("deceasedAt")).containsOnlyNulls();
        var dead = api().get().uri("/api/v1/patients?deceased=true").header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) dead.get("content")).extracting(p -> p.get("name")).contains("سيمبا");

        var byName = api().get().uri(b -> b.path("/api/v1/patients").queryParam("q", "{q}").build("MAX"))
            .header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) byName.get("content")).extracting(p -> p.get("name")).containsExactly("Max");

        var bad = api().get().uri("/api/v1/patients?sort=nope,asc").header("Authorization", "Bearer " + desk).retrieve().toEntity(Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bad.getBody()).containsEntry("code", "bad_sort");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createUpdateAndValidate() {
        var desk = login(api(), DESK, PASSWORD);
        var created = api().post().uri("/api/v1/patients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("clientId", SARAH, "name", "Coco", "species", "dog", "breed", "Poodle"))
            .retrieve().toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).containsEntry("sex", "unknown").containsEntry("version", 0).containsEntry("clientId", SARAH);
        var id = (String) created.getBody().get("id");

        var updated = api().put().uri("/api/v1/patients/" + id).header("Authorization", "Bearer " + desk)
            .header("If-Match", "0").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("clientId", SARAH, "name", "Coco", "species", "dog", "sex", "female_spayed", "color", "white"))
            .retrieve().toEntity(Map.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("sex", "female_spayed").containsEntry("version", 1);

        var stale = api().put().uri("/api/v1/patients/" + id).header("Authorization", "Bearer " + desk)
            .header("If-Match", "0").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("clientId", SARAH, "name", "Coco", "species", "dog"))
            .retrieve().toEntity(Map.class);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(stale.getBody()).containsEntry("code", "stale_update");

        // owner change through PUT is refused: that is a transfer
        var ownerViaPut = api().put().uri("/api/v1/patients/" + id).header("Authorization", "Bearer " + desk)
            .header("If-Match", "1").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("clientId", FATMA, "name", "Coco", "species", "dog"))
            .retrieve().toEntity(Map.class);
        assertThat(ownerViaPut.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ownerViaPut.getBody()).containsEntry("code", "patient.owner_change_is_transfer");
        assertThat((Map<String, String>) ownerViaPut.getBody().get("fields")).containsKey("clientId");

        // missing species is a validation error on the field
        Map<String, Object> noSpecies = new HashMap<>();
        noSpecies.put("clientId", SARAH);
        noSpecies.put("name", "Nameless");
        var invalid = api().post().uri("/api/v1/patients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(noSpecies).retrieve().toEntity(Map.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody()).containsEntry("code", "validation_failed");
        assertThat((Map<String, String>) invalid.getBody().get("fields")).containsKey("species");

        // deceased: a date, still readable, listed as deceased
        var deceased = api().post().uri("/api/v1/patients/" + id + "/deceased").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("date", "2026-09-01")).retrieve().toEntity(Map.class);
        assertThat(deceased.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deceased.getBody()).containsEntry("deceasedAt", "2026-09-01");
        var detail = api().get().uri("/api/v1/patients/" + id).header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((Map<String, Object>) detail.get("patient")).containsEntry("deceasedAt", "2026-09-01");
    }

    // ---- 3.2 transfer records the previous client -------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void transferKeepsHistory() {
        var desk = login(api(), DESK, PASSWORD);
        var vet = login(api(), VET, PASSWORD);
        var id = (String) api().post().uri("/api/v1/patients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("clientId", AHMED, "name", "Traveller", "species", "cat"))
            .retrieve().body(Map.class).get("id");
        api().post().uri("/api/v1/patients/" + id + "/weights").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("measuredAt", "2026-09-10T09:00:00+03:00", "weightKg", 3.4))
            .retrieve().toBodilessEntity();
        api().post().uri("/api/v1/patients/" + id + "/allergies").header("Authorization", "Bearer " + vet)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("substance", "Fish", "severity", "mild"))
            .retrieve().toBodilessEntity();

        var same = api().post().uri("/api/v1/patients/" + id + "/transfer").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("clientId", AHMED)).retrieve().toEntity(Map.class);
        assertThat(same.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(same.getBody()).containsEntry("code", "patient.transfer_same_client");

        var moved = api().post().uri("/api/v1/patients/" + id + "/transfer").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("clientId", FATMA)).retrieve().toEntity(Map.class);
        assertThat(moved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moved.getBody()).containsEntry("clientId", FATMA).containsEntry("previousClientId", AHMED);

        // records follow the patient
        var detail = api().get().uri("/api/v1/patients/" + id).header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((Map<String, Object>) detail.get("latestWeight")).containsEntry("weightKg", 3.4);
        assertThat((List<Map<String, Object>>) detail.get("activeAllergies")).extracting(a -> a.get("substance")).containsExactly("Fish");
        var underFatma = api().get().uri("/api/v1/patients?clientId=" + FATMA).header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) underFatma.get("content")).extracting(p -> p.get("id")).contains(id);

        // an unknown target is not found; nothing changed
        var nowhere = api().post().uri("/api/v1/patients/" + id + "/transfer").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("clientId", UUID.randomUUID().toString())).retrieve().toEntity(Map.class);
        assertThat(nowhere.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(nowhere.getBody()).containsEntry("code", "client.not_found");

        // a deceased patient stays with the owner who lost it
        api().post().uri("/api/v1/patients/" + id + "/deceased").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("date", "2026-09-15")).retrieve().toBodilessEntity();
        var dead = api().post().uri("/api/v1/patients/" + id + "/transfer").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("clientId", AHMED)).retrieve().toEntity(Map.class);
        assertThat(dead.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(dead.getBody()).containsEntry("code", "patient.transfer_deceased");
    }

    @Test
    @SuppressWarnings("unchecked")
    void weightsListNewestFirstAndHeaderShowsLatest() {
        var desk = login(api(), DESK, PASSWORD);
        var id = (String) api().post().uri("/api/v1/patients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("clientId", SARAH, "name", "Scale", "species", "dog"))
            .retrieve().body(Map.class).get("id");
        // older weight recorded second: ordering is by measured_at, not by insert order
        api().post().uri("/api/v1/patients/" + id + "/weights").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("measuredAt", "2026-08-01T10:00:00+03:00", "weightKg", 12.5))
            .retrieve().toBodilessEntity();
        api().post().uri("/api/v1/patients/" + id + "/weights").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("measuredAt", "2026-05-01T10:00:00+03:00", "weightKg", 11.0))
            .retrieve().toBodilessEntity();

        var weights = api().get().uri("/api/v1/patients/" + id + "/weights").header("Authorization", "Bearer " + desk).retrieve().body(List.class);
        assertThat((List<Map<String, Object>>) weights).extracting(w -> w.get("weightKg")).containsExactly(12.5, 11.0);
        var detail = api().get().uri("/api/v1/patients/" + id).header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((Map<String, Object>) detail.get("latestWeight")).containsEntry("weightKg", 12.5);

        var zero = api().post().uri("/api/v1/patients/" + id + "/weights").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("measuredAt", "2026-08-02T10:00:00+03:00", "weightKg", 0))
            .retrieve().toEntity(Map.class);
        assertThat(zero.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) zero.getBody().get("fields")).containsKey("weightKg");
    }

    // ---- 3.5 allergies: retract works, edit and delete are refused by the database ----------------

    @Test
    @SuppressWarnings("unchecked")
    void allergiesAreRetractedNeverEditedOrDeleted() throws SQLException {
        var desk = login(api(), DESK, PASSWORD);
        var vet = login(api(), VET, PASSWORD);

        // front desk never writes clinical data
        var deskWrite = api().post().uri("/api/v1/patients/" + REX + "/allergies").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("substance", "Beef", "severity", "mild"))
            .retrieve().toEntity(String.class);
        assertThat(deskWrite.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var added = api().post().uri("/api/v1/patients/" + REX + "/allergies").header("Authorization", "Bearer " + vet)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("substance", "Beef", "reaction", "vomiting", "severity", "moderate"))
            .retrieve().toEntity(Map.class);
        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(added.getBody()).containsEntry("notedBy", "00000000-0000-7000-8000-000000000102").containsEntry("retractedAt", null);
        var allergyId = (String) added.getBody().get("id");

        var retracted = api().post().uri("/api/v1/patients/" + REX + "/allergies/" + allergyId + "/retract")
            .header("Authorization", "Bearer " + vet).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("reason", "entered on the wrong patient")).retrieve().toEntity(Map.class);
        assertThat(retracted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retracted.getBody()).containsEntry("retractedReason", "entered on the wrong patient");
        assertThat(retracted.getBody().get("retractedAt")).isNotNull();

        var again = api().post().uri("/api/v1/patients/" + REX + "/allergies/" + allergyId + "/retract")
            .header("Authorization", "Bearer " + vet).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("reason", "twice")).retrieve().toEntity(Map.class);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody()).containsEntry("code", "allergy.already_retracted");

        // the retracted row stays in the full list and leaves the banner
        var all = (List<Map<String, Object>>) api().get().uri("/api/v1/patients/" + REX + "/allergies")
            .header("Authorization", "Bearer " + vet).retrieve().body(List.class);
        assertThat(all).extracting(a -> a.get("id")).contains(allergyId, REX_PENICILLIN);
        var detail = api().get().uri("/api/v1/patients/" + REX).header("Authorization", "Bearer " + vet).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) detail.get("activeAllergies")).extracting(a -> a.get("id"))
            .contains(REX_PENICILLIN).doesNotContain(allergyId);

        // the database refuses any other change from the application role, retracted or not
        try (var app = appDataSource.getConnection()) {
            app.setAutoCommit(false);
            for (var sql : List.of(
                    "UPDATE patient_allergies SET substance = 'Chicken' WHERE id = '" + REX_PENICILLIN + "'",
                    "UPDATE patient_allergies SET severity = 'mild' WHERE id = '" + REX_PENICILLIN + "'",
                    "UPDATE patient_allergies SET substance = 'Chicken' WHERE id = '" + allergyId + "'",
                    "DELETE FROM patient_allergies WHERE id = '" + REX_PENICILLIN + "'",
                    "DELETE FROM patient_allergies WHERE id = '" + allergyId + "'")) {
                try (var st = app.createStatement()) {
                    st.execute("SELECT set_config('app.practice_id', '" + PRACTICE_ID + "', true)");
                    assertThatThrownBy(() -> st.execute(sql)).as(sql)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("patient_allergies:");
                }
                app.rollback();
            }
            // and it cannot drop the guard or call it by hand
            try (var st = app.createStatement()) {
                assertThatThrownBy(() -> st.execute("DROP FUNCTION patient_allergy_guard() CASCADE"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("must be owner");
            }
            app.rollback();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void weightsAreVoidedNeverEditedOrDeleted() throws SQLException {
        var desk = login(api(), DESK, PASSWORD);
        var id = (String) api().post().uri("/api/v1/patients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("clientId", SARAH, "name", "Typo", "species", "dog"))
            .retrieve().body(Map.class).get("id");
        var first = (String) api().post().uri("/api/v1/patients/" + id + "/weights").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("measuredAt", "2026-08-01T10:00:00+03:00", "weightKg", 12.5))
            .retrieve().body(Map.class).get("id");
        var wrong = (String) api().post().uri("/api/v1/patients/" + id + "/weights").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("measuredAt", "2026-09-01T10:00:00+03:00", "weightKg", 125.0))
            .retrieve().body(Map.class).get("id");

        var voided = api().post().uri("/api/v1/patients/" + id + "/weights/" + wrong + "/void")
            .header("Authorization", "Bearer " + desk).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("reason", "decimal point slipped")).retrieve().toEntity(Map.class);
        assertThat(voided.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(voided.getBody()).containsEntry("voidReason", "decimal point slipped");
        assertThat(voided.getBody().get("voidedAt")).isNotNull();

        var again = api().post().uri("/api/v1/patients/" + id + "/weights/" + wrong + "/void")
            .header("Authorization", "Bearer " + desk).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("reason", "twice")).retrieve().toEntity(Map.class);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody()).containsEntry("code", "weight.already_voided");

        // the voided row stays in the list; the header falls back to the previous measurement
        var weights = (List<Map<String, Object>>) api().get().uri("/api/v1/patients/" + id + "/weights")
            .header("Authorization", "Bearer " + desk).retrieve().body(List.class);
        assertThat(weights).extracting(w -> w.get("id")).containsExactly(wrong, first);
        var detail = api().get().uri("/api/v1/patients/" + id).header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((Map<String, Object>) detail.get("latestWeight")).containsEntry("id", first);

        // a weight on another patient is not reachable through this one
        var elsewhere = api().post().uri("/api/v1/patients/" + REX + "/weights/" + first + "/void")
            .header("Authorization", "Bearer " + desk).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("reason", "wrong path")).retrieve().toEntity(Map.class);
        assertThat(elsewhere.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // the database refuses any other change from the application role
        try (var app = appDataSource.getConnection()) {
            app.setAutoCommit(false);
            for (var sql : List.of(
                    "UPDATE patient_weights SET weight_kg = 13 WHERE id = '" + first + "'",
                    "UPDATE patient_weights SET measured_at = now() WHERE id = '" + first + "'",
                    "UPDATE patient_weights SET weight_kg = 13 WHERE id = '" + wrong + "'",
                    "DELETE FROM patient_weights WHERE id = '" + first + "'",
                    "DELETE FROM patient_weights WHERE id = '" + wrong + "'")) {
                try (var st = app.createStatement()) {
                    st.execute("SELECT set_config('app.practice_id', '" + PRACTICE_ID + "', true)");
                    assertThatThrownBy(() -> st.execute(sql)).as(sql)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("patient_weights:");
                }
                app.rollback();
            }
            try (var st = app.createStatement()) {
                assertThatThrownBy(() -> st.execute("DROP FUNCTION patient_weight_guard() CASCADE"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("must be owner");
            }
            app.rollback();
        }
    }

    @Test
    void unknownPatientIsNotFound() {
        var desk = login(api(), DESK, PASSWORD);
        var r = api().get().uri("/api/v1/patients/" + UUID.randomUUID()).header("Authorization", "Bearer " + desk).retrieve().toEntity(Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).containsEntry("code", "patient.not_found");
    }
}
