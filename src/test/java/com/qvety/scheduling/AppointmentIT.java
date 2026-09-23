package com.qvety.scheduling;

import static com.qvety.ApiTestSupport.ADMIN;
import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.PASSWORD;
import static com.qvety.ApiTestSupport.TECH;
import static com.qvety.ApiTestSupport.client;
import static com.qvety.ApiTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.qvety.TestcontainersConfig;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
 * Part 10. The overlap rule belongs to the database, so it is exercised through the API rather than
 * unit-tested: what matters is that a double booking answers 409 and writes nothing. The transition
 * table is walked move by move. The cross-tenant case lives in TenantIsolationIT beside the other tables.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class AppointmentIT {

    static final String CAIRO = "Africa/Cairo";
    static final String VET = "00000000-0000-7000-8000-000000000102";
    static final String ADMIN_VET = "00000000-0000-7000-8000-000000000101";
    static final String TECH_USER = "00000000-0000-7000-8000-000000000103";
    static final String ROOM = "00000000-0000-7000-8000-000000000601";
    static final String ROOM_2 = "00000000-0000-7000-8000-000000000602";
    static final String INACTIVE_ROOM = "00000000-0000-7000-8000-000000000604";
    static final String TYPE = "00000000-0000-7000-8000-000000000611";
    static final String PATIENT = "00000000-0000-7000-8000-000000000401";
    static final String PATIENT_2 = "00000000-0000-7000-8000-000000000403";

    @LocalServerPort
    int port;

    RestClient api() {
        return client(port);
    }

    /**
     * A free slot on a day of this test's own, well clear of the seeded one. Every test gets a different
     * day so that a day-scoped assertion never sees another test's bookings, whatever order they run in.
     */
    private static OffsetDateTime slotAt(int dayOffset, int hour, int minute) {
        // The dayOffset-th open day from the anchor. Counting open days rather than adding to the date
        // keeps the days distinct: adding first and then stepping over Friday lands two offsets on one day.
        var day = LocalDate.now(ZoneId.of(CAIRO)).plusDays(30);
        for (int open = 0; ; day = day.plusDays(1)) {
            if (day.getDayOfWeek() == DayOfWeek.FRIDAY) {   // the clinic is shut
                continue;
            }
            if (open++ == dayOffset) {
                break;
            }
        }
        return day.atTime(hour, minute).atZone(ZoneId.of(CAIRO)).toOffsetDateTime();
    }

    /**
     * A slot on today's date, whatever the hour. The board covers the whole of today, so an hour already
     * past still belongs on it; anchoring to "now plus a few hours" would fall into tomorrow late at night.
     */
    private static OffsetDateTime todayAt(int hour) {
        return LocalDate.now(ZoneId.of(CAIRO)).atTime(hour, 0).atZone(ZoneId.of(CAIRO)).toOffsetDateTime();
    }

    private static Map<String, Object> booking(String patient, String vet, String room, OffsetDateTime start, int minutes) {
        var body = new HashMap<String, Object>();
        body.put("patientId", patient);
        body.put("veterinarianId", vet);
        body.put("roomId", room);
        body.put("appointmentTypeId", TYPE);
        body.put("start", start.toString());
        body.put("durationMinutes", minutes);
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String token, Map<String, Object> body) {
        return api().post().uri("/api/v1/appointments").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(Map.class).getBody();
    }

    private org.springframework.http.ResponseEntity<Map> postRaw(String token, Map<String, Object> body) {
        return api().post().uri("/api/v1/appointments").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(Map.class);
    }

    private org.springframework.http.ResponseEntity<Map> status(String token, String id, String next) {
        return api().post().uri("/api/v1/appointments/" + id + "/status").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("status", next)).retrieve().toEntity(Map.class);
    }

    @SuppressWarnings("unchecked")
    private static String idOf(Map<String, Object> saved) {
        return (String) ((Map<String, Object>) saved.get("appointment")).get("id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void overlapIsRefusedPerVeterinarianAndPerRoom() {
        var desk = login(api(), DESK, PASSWORD);
        var start = slotAt(1, 12, 0);

        var first = postRaw(desk, booking(PATIENT, VET, ROOM, start, 30));
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var firstId = idOf(first.getBody());

        // same vet, different room, overlapping by fifteen minutes
        var sameVet = postRaw(desk, booking(PATIENT_2, VET, ROOM_2, start.plusMinutes(15), 30));
        assertThat(sameVet.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sameVet.getBody()).containsEntry("code", "appointment.overlap");

        // same room, different vet, overlapping
        var sameRoom = postRaw(desk, booking(PATIENT_2, ADMIN_VET, ROOM, start.plusMinutes(15), 30));
        assertThat(sameRoom.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sameRoom.getBody()).containsEntry("code", "appointment.overlap");

        // back to back is not an overlap: the range is half open
        var after = postRaw(desk, booking(PATIENT_2, VET, ROOM, start.plusMinutes(30), 30));
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // nothing extra was written by the two refusals
        List<Map<String, Object>> day = api().get()
            .uri("/api/v1/appointments?date=" + start.atZoneSameInstant(ZoneId.of(CAIRO)).toLocalDate())
            .header("Authorization", "Bearer " + desk).retrieve().body(List.class);
        assertThat(day).hasSize(2);

        status(desk, firstId, "cancelled");
        status(desk, idOf(after.getBody()), "cancelled");
    }

    @Test
    void cancelledSlotIsReusable() {
        var desk = login(api(), DESK, PASSWORD);
        var start = slotAt(2, 13, 0);

        var first = postRaw(desk, booking(PATIENT, VET, ROOM, start, 30));
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = idOf(first.getBody());

        // taken
        assertThat(postRaw(desk, booking(PATIENT_2, VET, ROOM, start, 30)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        assertThat(status(desk, id, "cancelled").getStatusCode()).isEqualTo(HttpStatus.OK);

        // free again, and the cancelled row is still there
        var second = postRaw(desk, booking(PATIENT_2, VET, ROOM, start, 30));
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var cancelled = api().get().uri("/api/v1/appointments/" + id).header("Authorization", "Bearer " + desk)
            .retrieve().toEntity(Map.class);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody()).containsEntry("status", "cancelled");

        status(desk, idOf(second.getBody()), "cancelled");
    }

    @Test
    void transitionsFollowTheTable() {
        var desk = login(api(), DESK, PASSWORD);
        var id = idOf(post(desk, booking(PATIENT, VET, ROOM, slotAt(3, 14, 0), 30)));

        // scheduled -> in_progress is not a move the table allows
        var skipped = status(desk, id, "in_progress");
        assertThat(skipped.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(skipped.getBody()).containsEntry("code", "appointment.bad_transition");

        assertThat(status(desk, id, "checked_in").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(status(desk, id, "checked_in").getStatusCode()).isEqualTo(HttpStatus.OK);   // idempotent
        assertThat(status(desk, id, "no_show").getStatusCode()).isEqualTo(HttpStatus.CONFLICT); // only from scheduled
        assertThat(status(desk, id, "in_progress").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(status(desk, id, "cancelled").getStatusCode()).isEqualTo(HttpStatus.CONFLICT); // too late to cancel
        assertThat(status(desk, id, "completed").getStatusCode()).isEqualTo(HttpStatus.OK);

        // completed is terminal
        assertThat(status(desk, id, "scheduled").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(status(desk, id, "in_progress").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkInRecordsTheTimeAndTheBoardCountsTheMinutes() {
        var desk = login(api(), DESK, PASSWORD);
        var id = idOf(post(desk, booking(PATIENT, VET, ROOM_2, todayAt(20), 30)));

        var before = api().get().uri("/api/v1/appointments/" + id).header("Authorization", "Bearer " + desk)
            .retrieve().body(Map.class);
        assertThat(before.get("checkedInAt")).isNull();

        status(desk, id, "checked_in");
        var after = api().get().uri("/api/v1/appointments/" + id).header("Authorization", "Bearer " + desk)
            .retrieve().body(Map.class);
        assertThat(after.get("checkedInAt")).isNotNull();

        var board = api().get().uri("/api/v1/schedule/board").header("Authorization", "Bearer " + desk)
            .retrieve().body(Map.class);
        var waiting = (List<Map<String, Object>>) board.get("waiting");
        var mine = waiting.stream().filter(r -> id.equals(r.get("id"))).findFirst().orElseThrow();
        assertThat((Integer) mine.get("minutesWaiting")).isNotNull().isGreaterThanOrEqualTo(0);

        status(desk, id, "cancelled");
    }

    @Test
    @SuppressWarnings("unchecked")
    void walkInIsCheckedInAndKeepsItsOrigin() {
        var desk = login(api(), DESK, PASSWORD);
        var body = booking(PATIENT, VET, ROOM_2, todayAt(21), 20);
        body.put("walkIn", true);

        var saved = post(desk, body);
        var appointment = (Map<String, Object>) saved.get("appointment");
        assertThat(appointment).containsEntry("status", "checked_in").containsEntry("origin", "walk_in");
        assertThat(appointment.get("checkedInAt")).isNotNull();
        var id = (String) appointment.get("id");

        // an update cannot turn it back into a booked appointment
        var edit = booking(PATIENT, VET, ROOM_2, todayAt(22), 20);
        edit.put("walkIn", false);
        var updated = api().put().uri("/api/v1/appointments/" + id).header("Authorization", "Bearer " + desk)
            .header("If-Match", appointment.get("version").toString())
            .contentType(MediaType.APPLICATION_JSON).body(edit).retrieve().toEntity(Map.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) updated.getBody().get("appointment")).containsEntry("origin", "walk_in");

        status(desk, id, "cancelled");
    }

    @Test
    @SuppressWarnings("unchecked")
    void outsideOpeningHoursWarnsButSaves() {
        var desk = login(api(), DESK, PASSWORD);

        var late = postRaw(desk, booking(PATIENT, VET, ROOM_2, slotAt(8, 23, 0), 30));
        assertThat(late.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((List<String>) late.getBody().get("warnings")).containsExactly("appointment.outside_hours");

        var inHours = postRaw(desk, booking(PATIENT, VET, ROOM_2, slotAt(8, 11, 0), 30));
        assertThat(inHours.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((List<String>) inHours.getBody().get("warnings")).isEmpty();

        status(desk, idOf(late.getBody()), "cancelled");
        status(desk, idOf(inHours.getBody()), "cancelled");
    }

    @Test
    @SuppressWarnings("unchecked")
    void bookingChecksTheVeterinarianFlagAndActiveReferences() {
        var desk = login(api(), DESK, PASSWORD);
        var start = slotAt(4, 16, 0);

        var technician = postRaw(desk, booking(PATIENT, TECH_USER, ROOM, start, 30));
        assertThat(technician.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) technician.getBody().get("fields")).containsKey("veterinarianId");

        var closedRoom = postRaw(desk, booking(PATIENT, VET, INACTIVE_ROOM, start, 30));
        assertThat(closedRoom.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) closedRoom.getBody().get("fields")).containsKey("roomId");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dayGridGroupsByVeterinarianAndCarriesTheTimezone() {
        var desk = login(api(), DESK, PASSWORD);
        var start = slotAt(5, 17, 0);
        var a = postRaw(desk, booking(PATIENT, VET, ROOM, start, 30));
        var b = postRaw(desk, booking(PATIENT_2, ADMIN_VET, ROOM_2, start, 30));
        var date = start.atZoneSameInstant(ZoneId.of(CAIRO)).toLocalDate();

        var day = api().get().uri("/api/v1/schedule/day?date=" + date).header("Authorization", "Bearer " + desk)
            .retrieve().body(Map.class);
        assertThat(day).containsEntry("timezone", CAIRO).containsEntry("date", date.toString());
        assertThat(day.get("opens")).isNotNull();
        var columns = (List<Map<String, Object>>) day.get("columns");
        assertThat(columns).hasSize(2);
        assertThat(columns).extracting(c -> c.get("veterinarianId")).containsExactlyInAnyOrder(VET, ADMIN_VET);
        assertThat(columns).allSatisfy(c -> assertThat((List<?>) c.get("appointments")).hasSize(1));

        status(desk, idOf(a.getBody()), "cancelled");
        status(desk, idOf(b.getBody()), "cancelled");
    }

    @Test
    @SuppressWarnings("unchecked")
    void boardGroupsByStatusAndOnlyShowsToday() {
        var desk = login(api(), DESK, PASSWORD);
        var board = api().get().uri("/api/v1/schedule/board").header("Authorization", "Bearer " + desk)
            .retrieve().body(Map.class);

        // the dev seed puts one appointment in each state today
        assertThat((List<Map<String, Object>>) board.get("waiting")).isNotEmpty()
            .allSatisfy(r -> assertThat(r).containsEntry("status", "checked_in"));
        assertThat((List<Map<String, Object>>) board.get("inExam")).isNotEmpty()
            .allSatisfy(r -> assertThat(r).containsEntry("status", "in_progress"));
        assertThat((List<Map<String, Object>>) board.get("done")).isNotEmpty()
            .allSatisfy(r -> assertThat(r).containsEntry("status", "completed"));

        // a card carries what it needs to be read from across the room
        var card = ((List<Map<String, Object>>) board.get("waiting")).getFirst();
        assertThat(card).containsKeys("patientName", "clientName", "veterinarianName", "roomName", "minutesWaiting");

        // cancelled rows never appear
        var all = List.of("waiting", "inExam", "done").stream()
            .flatMap(k -> ((List<Map<String, Object>>) board.get(k)).stream()).toList();
        assertThat(all).noneSatisfy(r -> assertThat(r).containsEntry("status", "cancelled"));
    }

    @Test
    void readingIsOpenToEveryoneWritingIsNot() {
        var tech = login(api(), TECH, PASSWORD);
        assertThat(api().get().uri("/api/v1/schedule/board").header("Authorization", "Bearer " + tech)
            .retrieve().toEntity(String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api().get().uri("/api/v1/appointments").header("Authorization", "Bearer " + tech)
            .retrieve().toEntity(String.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        var booked = postRaw(tech, booking(PATIENT, VET, ROOM, slotAt(6, 18, 0), 30));
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(booked.getBody()).containsEntry("code", "forbidden");
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyoneCanReadTheVeterinariansToBookThem() {
        // the full user record stays admin-only; the front desk still has to pick a vet
        var desk = login(api(), DESK, PASSWORD);
        assertThat(api().get().uri("/api/v1/users").header("Authorization", "Bearer " + desk)
            .retrieve().toEntity(String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        List<Map<String, Object>> vets = api().get().uri("/api/v1/users/veterinarians")
            .header("Authorization", "Bearer " + desk).retrieve().body(List.class);
        assertThat(vets).isNotEmpty()
            .allSatisfy(v -> assertThat(v).containsOnlyKeys("id", "fullName"));   // no email, no licence, no status
        assertThat(vets).extracting(v -> v.get("id")).contains(VET, ADMIN_VET).doesNotContain(TECH_USER);
    }

    @Test
    void adminCanBookToo() {
        var admin = login(api(), ADMIN, PASSWORD);
        var saved = postRaw(admin, booking(PATIENT, VET, ROOM, slotAt(7, 19, 0), 30));
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        status(admin, idOf(saved.getBody()), "cancelled");
    }
}
