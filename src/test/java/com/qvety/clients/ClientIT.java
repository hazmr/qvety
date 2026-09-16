package com.qvety.clients;

import static com.qvety.ApiTestSupport.ADMIN;
import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.PASSWORD;
import static com.qvety.ApiTestSupport.TECH;
import static com.qvety.ApiTestSupport.client;
import static com.qvety.ApiTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.qvety.TestcontainersConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** Part 06: clients CRUD, paging, archive, duplicate warnings, error shape, and the tenant boundary. Part 07: folded search. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class ClientIT {

    static final String SEEDED_AHMED = "00000000-0000-7000-8000-000000000301";

    @LocalServerPort
    int port;

    RestClient api() {
        return client(port);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createListUpdateArchive() {
        var desk = login(api(), DESK, PASSWORD);

        var created = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "  Karim Fathy ", "phone", "+201099990001", "email", "Karim@Clients.Example.com"))
            .retrieve().toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var saved = (Map<String, Object>) created.getBody().get("client");
        var id = (String) saved.get("id");
        assertThat(saved).containsEntry("fullName", "Karim Fathy").containsEntry("email", "karim@clients.example.com")
            .containsEntry("version", 0);
        assertThat((List<?>) created.getBody().get("warnings")).isEmpty();

        // paging: size 2 gives 2 rows and the page block; size 1000 is capped at 100
        var page = api().get().uri("/api/v1/clients?size=2&sort=fullName,asc").header("Authorization", "Bearer " + desk)
            .retrieve().body(Map.class);
        assertThat((List<?>) page.get("content")).hasSize(2);
        var pageInfo = (Map<String, Object>) page.get("page");
        assertThat(pageInfo).containsEntry("size", 2);
        var big = api().get().uri("/api/v1/clients?size=1000").header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((Map<String, Object>) big.get("page")).containsEntry("size", 100);

        // search by name fragment and by exact phone
        var byName = api().get().uri("/api/v1/clients?q=karim").header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) byName.get("content")).extracting(c -> c.get("id")).contains(id);
        var byPhone = api().get().uri(b -> b.path("/api/v1/clients").queryParam("q", "{q}").build("+201099990001"))
            .header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) byPhone.get("content")).extracting(c -> c.get("id")).containsExactly(id);

        // update with the right version, then with the stale one
        var updated = api().put().uri("/api/v1/clients/" + id).header("Authorization", "Bearer " + desk)
            .header("If-Match", "0").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Karim Fathy", "phone", "+201099990001", "address", "Maadi"))
            .retrieve().toEntity(Map.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) updated.getBody().get("client")).containsEntry("address", "Maadi").containsEntry("version", 1);

        var stale = api().put().uri("/api/v1/clients/" + id).header("Authorization", "Bearer " + desk)
            .header("If-Match", "0").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Karim Fathy", "phone", "+201099990001", "address", "Old tab"))
            .retrieve().toEntity(Map.class);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(stale.getBody()).containsEntry("code", "stale_update");

        // archive: row stays, leaves the default list, edit refused
        var archived = api().post().uri("/api/v1/clients/" + id + "/archive").header("Authorization", "Bearer " + desk)
            .retrieve().body(Map.class);
        assertThat(archived.get("archivedAt")).isNotNull();
        var stillReadable = api().get().uri("/api/v1/clients/" + id).header("Authorization", "Bearer " + desk).retrieve().toEntity(Map.class);
        assertThat(stillReadable.getStatusCode()).isEqualTo(HttpStatus.OK);
        var listAfter = api().get().uri("/api/v1/clients?q=karim").header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((List<?>) listAfter.get("content")).isEmpty();
        var editArchived = api().put().uri("/api/v1/clients/" + id).header("Authorization", "Bearer " + desk)
            .header("If-Match", "1").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Karim Fathy", "phone", "+201099990001"))
            .retrieve().toEntity(Map.class);
        assertThat(editArchived.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(editArchived.getBody()).containsEntry("code", "client.archived");

        // part 06c: absent from the plain list and both search branches, present in all three with the flag
        for (var q : List.of("", "karim", "+201099990001")) {
            var hidden = api().get().uri(u -> u.path("/api/v1/clients").queryParam("q", "{q}").build(q))
                .header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
            assertThat((List<Map<String, Object>>) hidden.get("content")).as("hidden " + q).extracting(c -> c.get("id")).doesNotContain(id);
            var shown = api().get().uri(u -> u.path("/api/v1/clients").queryParam("q", "{q}").queryParam("includeArchived", true).build(q))
                .header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
            var row = ((List<Map<String, Object>>) shown.get("content")).stream().filter(c -> id.equals(c.get("id"))).findFirst();
            assertThat(row).as("shown " + q).isPresent();
            assertThat(row.get().get("archivedAt")).isNotNull();
        }

        // unarchive: back in the default list, edits accepted, a second press is harmless
        var restored = api().post().uri("/api/v1/clients/" + id + "/unarchive").header("Authorization", "Bearer " + desk)
            .retrieve().toEntity(Map.class);
        assertThat(restored.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restored.getBody().get("archivedAt")).isNull();
        var listRestored = api().get().uri("/api/v1/clients?q=karim").header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) listRestored.get("content")).extracting(c -> c.get("id")).contains(id);
        var current = api().get().uri("/api/v1/clients/" + id).header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        var editRestored = api().put().uri("/api/v1/clients/" + id).header("Authorization", "Bearer " + desk)
            .header("If-Match", String.valueOf(current.get("version"))).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Karim Fathy", "phone", "+201099990001", "address", "Back"))
            .retrieve().toEntity(Map.class);
        assertThat(editRestored.getStatusCode()).isEqualTo(HttpStatus.OK);
        var twice = api().post().uri("/api/v1/clients/" + id + "/unarchive").header("Authorization", "Bearer " + desk)
            .retrieve().toEntity(Map.class);
        assertThat(twice.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @SuppressWarnings("unchecked")
    void duplicateWarnsButDoesNotBlock() {
        var desk = login(api(), DESK, PASSWORD);
        var r = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Duplicate Case", "phone", "+201011111102"))   // seeded Fatma's phone
            .retrieve().toEntity(Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var warnings = (List<Map<String, Object>>) r.getBody().get("warnings");
        assertThat(warnings).extracting(w -> w.get("id")).contains("00000000-0000-7000-8000-000000000302");

        var sameName = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", " أحمد محمد علي حسن ", "phone", "+201099990002"))
            .retrieve().toEntity(Map.class);
        assertThat(sameName.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((List<Map<String, Object>>) sameName.getBody().get("warnings")).extracting(w -> w.get("id")).contains(SEEDED_AHMED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void errorShapeAndRoles() {
        var desk = login(api(), DESK, PASSWORD);
        // part 06c: a phone is required; email alone cannot be recalled
        for (var body : List.of(Map.of("fullName", "No Contact"), Map.of("fullName", "Email Only", "email", "only@clients.example.com"))) {
            var noPhone = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
                .header("Accept-Language", "ar-EG").contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toEntity(Map.class);
            assertThat(noPhone.getStatusCode()).as(body.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(noPhone.getBody()).containsEntry("code", "client.phone_required")
                .containsEntry("message", "أدخل رقم هاتف.");
            assertThat((Map<String, String>) noPhone.getBody().get("fields")).containsKey("phone");
        }

        Map<String, Object> invalid = new HashMap<>();
        invalid.put("fullName", "");
        invalid.put("email", "not-an-email");
        var validation = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON).body(invalid).retrieve().toEntity(Map.class);
        assertThat(validation.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(validation.getBody()).containsEntry("code", "validation_failed");
        assertThat((Map<String, String>) validation.getBody().get("fields")).containsKeys("fullName", "email");

        var badSort = api().get().uri("/api/v1/clients?sort=nope,asc").header("Authorization", "Bearer " + desk).retrieve().toEntity(Map.class);
        assertThat(badSort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badSort.getBody()).containsEntry("code", "bad_sort");

        var tech = login(api(), TECH, PASSWORD);
        var read = api().get().uri("/api/v1/clients").header("Authorization", "Bearer " + tech).retrieve().toEntity(String.class);
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        var write = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + tech)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("fullName", "T", "phone", "+201000000000"))
            .retrieve().toEntity(String.class);
        assertThat(write.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- part 07: folded search, E.164 phones ----------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void searchFoldsArabicSpelling() {
        var desk = login(api(), DESK, PASSWORD);
        var created = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "أحمد محمد", "phone", "+201099990007"))
            .retrieve().toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = (String) ((Map<String, Object>) created.getBody().get("client")).get("id");

        // bare alef finds hamza; the row is shown as typed
        var found = api().get().uri(b -> b.path("/api/v1/clients").queryParam("q", "{q}").build("احمد"))
            .header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        var rows = (List<Map<String, Object>>) found.get("content");
        assertThat(rows).extracting(c -> c.get("id")).contains(id, SEEDED_AHMED);
        assertThat(rows).filteredOn(c -> id.equals(c.get("id"))).extracting(c -> c.get("fullName")).containsExactly("أحمد محمد");

        // diacritics and ta marbuta fold the same way; seeded Fatma is فاطمة السيد إبراهيم
        var fatma = api().get().uri(b -> b.path("/api/v1/clients").queryParam("q", "{q}").build("فاطمه"))
            .header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) fatma.get("content")).extracting(c -> c.get("id"))
            .contains("00000000-0000-7000-8000-000000000302");

        // variant spelling is a duplicate warning, not a block
        var variant = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "احمد محمد", "phone", "+201099990003"))
            .retrieve().toEntity(Map.class);
        assertThat(variant.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((List<Map<String, Object>>) variant.getBody().get("warnings")).extracting(w -> w.get("id")).contains(id);
    }

    @Test
    @SuppressWarnings("unchecked")
    void phoneShapesMatchOneClient() {
        var desk = login(api(), DESK, PASSWORD);
        var created = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Phone Shapes", "phone", "0100 999 0008"))
            .retrieve().toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var saved = (Map<String, Object>) created.getBody().get("client");
        var id = (String) saved.get("id");
        assertThat(saved).containsEntry("phone", "0100 999 0008");   // shown as typed

        for (var shape : List.of("01009990008", "+201009990008", "0100 999 0008", "00201009990008")) {
            var r = api().get().uri(b -> b.path("/api/v1/clients").queryParam("q", "{q}").build(shape))
                .header("Authorization", "Bearer " + desk).retrieve().body(Map.class);
            assertThat((List<Map<String, Object>>) r.get("content")).as(shape).extracting(c -> c.get("id")).containsExactly(id);
        }

        // the secondary phone is searchable too, and a typed duplicate of it warns
        var second = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Second Line", "phone", "+201099990009", "phoneSecondary", "01009990008"))
            .retrieve().toEntity(Map.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((List<Map<String, Object>>) second.getBody().get("warnings")).extracting(w -> w.get("id")).contains(id);

        // missing trunk zero is rejected on the phone field
        var invalid = api().post().uri("/api/v1/clients").header("Authorization", "Bearer " + desk)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Bad Phone", "phone", "1012345678"))
            .retrieve().toEntity(Map.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody()).containsEntry("code", "phone.invalid");
        assertThat((Map<String, String>) invalid.getBody().get("fields")).containsKey("phone");
    }

    @Test
    void otherPracticeClientIsNotFound() {
        // practice B exists after TenantIsolationIT seeds it; here we only need an id that is not ours
        var admin = login(api(), ADMIN, PASSWORD);
        var r = api().get().uri("/api/v1/clients/" + UUID.randomUUID()).header("Authorization", "Bearer " + admin)
            .retrieve().toEntity(Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).containsEntry("code", "client.not_found");
    }
}
