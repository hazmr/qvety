package com.qvety.tenant;

import static com.qvety.ApiTestSupport.ADMIN;
import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.PASSWORD;
import static com.qvety.ApiTestSupport.PRACTICE_ID;
import static com.qvety.ApiTestSupport.client;
import static com.qvety.ApiTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qvety.TestcontainersConfig;
import com.qvety.users.User;
import com.qvety.users.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The tenant boundary, proven on every commit. Practice A is the dev seed; practice B is inserted
 * here as the owner. Every later part adds its tables to {@link #everyTenantTableIsIsolatedAndAudited}
 * automatically: the assertion is derived from the catalog, so a table that forgets
 * tenant_table_setup() fails the build.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class TenantIsolationIT {

    static final UUID PRACTICE_A = UUID.fromString(PRACTICE_ID);
    static final UUID PRACTICE_B = UUID.fromString("00000000-0000-7000-8000-000000000002");
    static final UUID ADMIN_A = UUID.fromString("00000000-0000-7000-8000-000000000101");
    static final UUID DESK_A = UUID.fromString("00000000-0000-7000-8000-000000000104");
    static final UUID ADMIN_B = UUID.fromString("00000000-0000-7000-8000-000000000201");
    static final String ADMIN_B_EMAIL = "admin@other.example.com";
    static final String HASH = "$2a$12$tRrkJgHmsapCuGvMgs1yT.kiQJf.56xF7z9UuX6b517QZJL7UY0E2";   // password123

    @LocalServerPort int port;
    @Autowired PostgreSQLContainer postgres;
    @Autowired DataSource appDataSource;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager txManager;
    @Autowired com.qvety.platform.ExportService exportService;

    @BeforeAll
    static void seedPracticeB(@Autowired PostgreSQLContainer postgres) throws SQLException {
        try (var owner = ownerConnection(postgres); var st = owner.createStatement()) {
            st.execute("""
                INSERT INTO practices (id, name, country, currency, locale, timezone)
                VALUES ('%s', 'Other Clinic', 'EG', 'EGP', 'ar-EG', 'Africa/Cairo') ON CONFLICT (id) DO NOTHING
                """.formatted(PRACTICE_B));
            st.execute("""
                INSERT INTO users (id, practice_id, phone, email, password_hash, full_name, role, is_veterinarian)
                VALUES ('%s', '%s', '+201000000201', '%s', '%s', 'Admin B', 'admin', true) ON CONFLICT (id) DO NOTHING
                """.formatted(ADMIN_B, PRACTICE_B, ADMIN_B_EMAIL, HASH));
        }
    }

    // ---- 4.1 API, repository, lazy proxy, native query -------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void apiNeverReturnsTheOtherPractice() {
        var tokenA = login(client(port), ADMIN, PASSWORD);
        var tokenB = login(client(port), ADMIN_B_EMAIL, PASSWORD);

        List<Map<String, Object>> usersA = client(port).get().uri("/api/v1/users")
            .header("Authorization", "Bearer " + tokenA).retrieve().body(List.class);
        List<Map<String, Object>> usersB = client(port).get().uri("/api/v1/users")
            .header("Authorization", "Bearer " + tokenB).retrieve().body(List.class);

        assertThat(usersA).extracting(u -> u.get("email")).doesNotContain(ADMIN_B_EMAIL).contains(ADMIN, DESK);
        assertThat(usersB).extracting(u -> u.get("email")).containsExactly(ADMIN_B_EMAIL);

        var meB = client(port).get().uri("/api/v1/me").header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
        assertThat(meB).containsEntry("email", ADMIN_B_EMAIL);

        var practiceB = client(port).get().uri("/api/v1/practice").header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
        assertThat(practiceB).containsEntry("id", PRACTICE_B.toString());

        // another practice's user by id: 404, never 403
        var cross = client(port).get().uri("/api/v1/users/" + DESK_A).header("Authorization", "Bearer " + tokenB)
            .retrieve().toEntity(String.class);
        assertThat(cross.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void repositoryUnderAIsBlindToB() {
        TenantContext.runAs(new TenantScope(PRACTICE_A, ADMIN_A), () -> {
            assertThat(users.findById(ADMIN_B)).isEmpty();
            assertThat(users.findAll()).extracting(User::getPracticeId).containsOnly(PRACTICE_A);
            return null;
        });
    }

    @Test
    void lazyProxyUnderACannotLoadB() {
        var tx = new TransactionTemplate(txManager);
        TenantContext.runAs(new TenantScope(PRACTICE_A, ADMIN_A), () -> {
            assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                var proxy = entityManager.getReference(User.class, ADMIN_B);   // no query yet
                proxy.getFullName();                                            // lazy load: RLS says no row
            })).isInstanceOf(EntityNotFoundException.class);
            return null;
        });
    }

    @Test
    void nativeQueryUnderAIsFilteredByPostgres() {
        var tx = new TransactionTemplate(txManager);
        TenantContext.runAs(new TenantScope(PRACTICE_A, ADMIN_A), () -> {
            var rows = tx.execute(status -> entityManager
                .createNativeQuery("SELECT id FROM users WHERE id = :id").setParameter("id", ADMIN_B).getResultList());
            assertThat(rows).isEmpty();
            var count = tx.execute(status -> entityManager
                .createNativeQuery("SELECT count(*) FROM users WHERE practice_id = :b").setParameter("b", PRACTICE_B).getSingleResult());
            assertThat(((Number) count).longValue()).isZero();
            return null;
        });
    }

    // ---- 4.2 no context -------------------------------------------------------------------------

    @Test
    void transactionWithoutTenantOrSystemContextThrows() {
        var tx = new TransactionTemplate(txManager);
        assertThatThrownBy(() -> tx.execute(status -> users.findAll()))
            .isInstanceOf(NoTenantException.class);
        // and with SystemContext the transaction opens but RLS shows nothing
        var seen = SystemContext.call(() -> tx.execute(status -> users.findAll()));
        assertThat(seen).isEmpty();
    }

    // ---- 4.3 every tenant table is in the lists -------------------------------------------------

    @Test
    void everyTenantTableIsIsolatedAndAudited() throws SQLException {
        try (var owner = ownerConnection(postgres); var st = owner.createStatement()) {
            Set<String> withPracticeId = new HashSet<>();
            try (var rs = st.executeQuery("""
                SELECT table_name FROM information_schema.columns
                WHERE table_schema = 'public' AND column_name = 'practice_id'""")) {
                while (rs.next()) withPracticeId.add(rs.getString(1));
            }
            Set<String> withPolicy = new HashSet<>();
            try (var rs = st.executeQuery("SELECT tablename FROM pg_policies WHERE schemaname = 'public' AND policyname = 'tenant_isolation'")) {
                while (rs.next()) withPolicy.add(rs.getString(1));
            }
            Set<String> forced = new HashSet<>();
            try (var rs = st.executeQuery("""
                SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public' AND c.relkind = 'r' AND c.relrowsecurity AND c.relforcerowsecurity""")) {
                while (rs.next()) forced.add(rs.getString(1));
            }
            Set<String> audited = new HashSet<>();
            try (var rs = st.executeQuery("""
                SELECT c.relname FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid
                WHERE t.tgname = 'audit' AND NOT t.tgisinternal""")) {
                while (rs.next()) audited.add(rs.getString(1));
            }

            assertThat(withPracticeId).isNotEmpty();
            // RLS list: every table with practice_id, no exceptions
            assertThat(withPolicy).containsExactlyInAnyOrderElementsOf(withPracticeId);
            assertThat(forced).containsAll(withPracticeId).contains("practices");
            // audit list: every tenant table except the log itself, plus practices
            var expectedAudited = new HashSet<>(withPracticeId);
            expectedAudited.remove("audit_log");
            expectedAudited.add("practices");
            assertThat(audited).containsExactlyInAnyOrderElementsOf(expectedAudited);
            // export list (part 11): derived from the same catalog, so the three sets cannot drift apart
            assertThat(exportService.tables()).containsExactlyInAnyOrderElementsOf(withPracticeId);
        }
    }

    // ---- part 06: clients ----------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void clientsAreInvisibleAcrossPractices() {
        var tokenA = login(client(port), ADMIN, PASSWORD);
        var tokenB = login(client(port), ADMIN_B_EMAIL, PASSWORD);
        var seededA = "00000000-0000-7000-8000-000000000301";

        var a = client(port).get().uri("/api/v1/clients/" + seededA).header("Authorization", "Bearer " + tokenA).retrieve().toEntity(Map.class);
        assertThat(a.getStatusCode()).isEqualTo(HttpStatus.OK);
        var b = client(port).get().uri("/api/v1/clients/" + seededA).header("Authorization", "Bearer " + tokenB).retrieve().toEntity(Map.class);
        assertThat(b.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var listB = client(port).get().uri("/api/v1/clients").header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
        assertThat((List<?>) listB.get("content")).isEmpty();

        var editB = client(port).put().uri("/api/v1/clients/" + seededA).header("Authorization", "Bearer " + tokenB)
            .header("If-Match", "0").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Hijacked", "phone", "+201000000000")).retrieve().toEntity(String.class);
        assertThat(editB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // part 07: neither search branch (folded name, E.164 phone) crosses the boundary
        for (var q : List.of("احمد", "+201011111101", "01011111101")) {
            var searchB = client(port).get().uri(u -> u.path("/api/v1/clients").queryParam("q", "{q}").build(q))
                .header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
            assertThat((List<?>) searchB.get("content")).as(q).isEmpty();
        }

        // part 06c: the archived view and unarchive stay inside the practice
        var archivedListB = client(port).get().uri("/api/v1/clients?includeArchived=true").header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
        assertThat((List<Map<String, Object>>) archivedListB.get("content")).extracting(c -> c.get("id")).doesNotContain(seededA);
        var unarchiveB = client(port).post().uri("/api/v1/clients/" + seededA + "/unarchive").header("Authorization", "Bearer " + tokenB)
            .retrieve().toEntity(String.class);
        assertThat(unarchiveB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- part 08: patients, weights, allergies ---------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void patientsAreInvisibleAcrossPractices() {
        var tokenA = login(client(port), ADMIN, PASSWORD);
        var tokenB = login(client(port), ADMIN_B_EMAIL, PASSWORD);
        var seededClientA = "00000000-0000-7000-8000-000000000301";
        var rex = "00000000-0000-7000-8000-000000000402";   // seeded, with a weight and an allergy
        var clientB = clientOfB(tokenB);

        // 3.3 read, list, and the child lists answer 404 or empty from practice B
        assertThat(client(port).get().uri("/api/v1/patients/" + rex).header("Authorization", "Bearer " + tokenA)
            .retrieve().toEntity(Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        for (var path : List.of("/api/v1/patients/" + rex, "/api/v1/patients/" + rex + "/weights", "/api/v1/patients/" + rex + "/allergies")) {
            var b = client(port).get().uri(path).header("Authorization", "Bearer " + tokenB).retrieve().toEntity(String.class);
            assertThat(b.getStatusCode()).as(path).isEqualTo(HttpStatus.NOT_FOUND);
        }
        var listB = client(port).get().uri("/api/v1/patients?clientId=" + seededClientA).header("Authorization", "Bearer " + tokenB)
            .retrieve().body(Map.class);
        assertThat((List<?>) listB.get("content")).isEmpty();
        var allB = client(port).get().uri("/api/v1/patients").header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
        assertThat((List<?>) allB.get("content")).isEmpty();

        // writes from B against A's patient: not found, nothing changed
        var transferB = client(port).post().uri("/api/v1/patients/" + rex + "/transfer").header("Authorization", "Bearer " + tokenB)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("clientId", clientB)).retrieve().toEntity(String.class);
        assertThat(transferB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        var weightB = client(port).post().uri("/api/v1/patients/" + rex + "/weights").header("Authorization", "Bearer " + tokenB)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("measuredAt", "2026-09-01T10:00:00+03:00", "weightKg", 1))
            .retrieve().toEntity(String.class);
        assertThat(weightB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 3.4 through the API: A cannot create a patient under B's client (RLS hides it: 404)
        var createA = client(port).post().uri("/api/v1/patients").header("Authorization", "Bearer " + tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("clientId", clientB, "name", "Smuggled", "species", "dog"))
            .retrieve().toEntity(Map.class);
        assertThat(createA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(createA.getBody()).containsEntry("code", "client.not_found");
        var transferA = client(port).post().uri("/api/v1/patients/" + rex + "/transfer").header("Authorization", "Bearer " + tokenA)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("clientId", clientB)).retrieve().toEntity(Map.class);
        assertThat(transferA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        archive(tokenB, clientB);
    }

    /** 3.4 at the database: the composite FK refuses a cross-practice client even for the owner, who bypasses RLS. */
    @Test
    void crossPracticeClientLinkFailsAtTheDatabase() throws SQLException {
        var tokenB = login(client(port), ADMIN_B_EMAIL, PASSWORD);
        var clientB = clientOfB(tokenB);
        var insert = "INSERT INTO patients (practice_id, client_id, name, species) VALUES ('" + PRACTICE_A + "', '" + clientB + "', 'Smuggled', 'dog')";
        try (var owner = ownerConnection(postgres); var st = owner.createStatement()) {
            assertThatThrownBy(() -> st.execute(insert))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("patients_client_same_practice");
        }
        try (var app = appDataSource.getConnection(); var st = app.createStatement()) {
            app.setAutoCommit(false);
            st.execute("SELECT set_config('app.practice_id', '" + PRACTICE_A + "', true)");
            assertThatThrownBy(() -> st.execute(insert))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("patients_client_same_practice");
            app.rollback();
        }
        archive(tokenB, clientB);
    }

    /** A client under practice B, created through the API. Callers archive it so B's default client list stays empty. */
    @SuppressWarnings("unchecked")
    private String clientOfB(String tokenB) {
        var created = client(port).post().uri("/api/v1/clients").header("Authorization", "Bearer " + tokenB)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "Client B " + UUID.randomUUID(), "phone", "+201000000311"))
            .retrieve().body(Map.class);
        return (String) ((Map<String, Object>) created.get("client")).get("id");
    }

    private void archive(String token, String clientId) {
        client(port).post().uri("/api/v1/clients/" + clientId + "/archive").header("Authorization", "Bearer " + token).retrieve().toBodilessEntity();
    }

    // ---- 4.5 practice row change is audited under its own id -------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void practiceAddressChangeIsAuditedUnderThePracticeId() {
        var tokenA = login(client(port), ADMIN, PASSWORD);
        client(port).put().uri("/api/v1/practice").header("Authorization", "Bearer " + tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Neighborhood Vet", "address", "Audit St 1", "taxRatePercent", 0))
            .retrieve().toBodilessEntity();

        var page = client(port).get().uri("/api/v1/audit?table=practices&rowId=" + PRACTICE_A)
            .header("Authorization", "Bearer " + tokenA).retrieve().body(Map.class);
        var rows = (List<Map<String, Object>>) page.get("content");
        assertThat(rows).isNotEmpty();
        var latest = rows.get(0);
        assertThat(latest).containsEntry("action", "update").containsEntry("tableName", "practices");
        assertThat((Map<String, Object>) latest.get("after")).containsEntry("address", "Audit St 1");
    }

    // ---- 4.6 what the application role cannot do -------------------------------------------------

    @Test
    void applicationRoleCannotEscalate() throws SQLException {
        try (var app = appDataSource.getConnection()) {
            app.setAutoCommit(false);
            for (var sql : List.of(
                    "UPDATE practices SET status = 'active' WHERE id = '" + PRACTICE_A + "'",
                    "UPDATE audit_log SET at = now()",
                    "DELETE FROM audit_log",
                    "SELECT audit_row()",
                    "SELECT tenant_table_setup('users')",
                    "INSERT INTO users (practice_id, phone, email, password_hash, full_name, role) VALUES ('" + PRACTICE_B
                        + "', '+201000000299', 'smuggled@other.example.com', 'x', 'X', 'front_desk')")) {
                try (var st = app.createStatement()) {
                    st.execute("SELECT set_config('app.practice_id', '" + PRACTICE_A + "', true)");
                    assertThatThrownBy(() -> st.execute(sql)).as(sql)
                        .isInstanceOf(SQLException.class)
                        .matches(e -> e.getMessage().contains("permission denied")
                            || e.getMessage().contains("row-level security policy"), "denied by Postgres");
                }
                app.rollback();
            }
        }
    }

    // ---- 4.7 audit rows are tenant-scoped --------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void practiceBCannotReadPracticeAAudit() {
        var tokenA = login(client(port), ADMIN, PASSWORD);
        client(port).put().uri("/api/v1/users/" + DESK_A).header("Authorization", "Bearer " + tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("phone", "+201000000104", "email", DESK, "fullName", "Mona Adel", "role", "front_desk", "veterinarian", false))
            .retrieve().toBodilessEntity();

        var tokenB = login(client(port), ADMIN_B_EMAIL, PASSWORD);
        var pageB = client(port).get().uri("/api/v1/audit?table=users&rowId=" + DESK_A)
            .header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
        assertThat((List<?>) pageB.get("content")).isEmpty();

        var allB = client(port).get().uri("/api/v1/audit").header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
        var rowsB = (List<Map<String, Object>>) allB.get("content");
        assertThat(rowsB).extracting(r -> r.get("rowId")).doesNotContain(DESK_A.toString(), ADMIN_A.toString());
    }

    // ---- part 09: reference data ---------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void referenceRowsAreInvisibleAcrossPractices() {
        var tokenA = login(client(port), ADMIN, PASSWORD);
        var tokenB = login(client(port), ADMIN_B_EMAIL, PASSWORD);
        var seededRoomA = "00000000-0000-7000-8000-000000000601";

        for (var path : List.of("/api/v1/reference/rooms", "/api/v1/reference/appointment-types", "/api/v1/reference/services")) {
            // practice A has its starter catalog; practice B was created without one and sees nothing
            assertThat((List<Object>) client(port).get().uri(path + "?includeInactive=true")
                .header("Authorization", "Bearer " + tokenA).retrieve().body(List.class)).isNotEmpty();
            assertThat((List<Object>) client(port).get().uri(path + "?includeInactive=true")
                .header("Authorization", "Bearer " + tokenB).retrieve().body(List.class)).isEmpty();
        }

        var readB = client(port).get().uri("/api/v1/reference/rooms/" + seededRoomA)
            .header("Authorization", "Bearer " + tokenB).retrieve().toEntity(String.class);
        assertThat(readB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var editB = client(port).put().uri("/api/v1/reference/rooms/" + seededRoomA)
            .header("Authorization", "Bearer " + tokenB).header("If-Match", "0")
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("name", "Hijacked"))
            .retrieve().toEntity(String.class);
        assertThat(editB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var deactivateB = client(port).post().uri("/api/v1/reference/rooms/" + seededRoomA + "/deactivate")
            .header("Authorization", "Bearer " + tokenB).retrieve().toEntity(String.class);
        assertThat(deactivateB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // B may use a name A already uses: the unique index is per practice
        var sameName = client(port).post().uri("/api/v1/reference/rooms")
            .header("Authorization", "Bearer " + tokenB).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "غرفة الكشف 1")).retrieve().toEntity(String.class);
        assertThat(sameName.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---- part 10: scheduling ---------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void appointmentsAndHoursAreInvisibleAcrossPractices() {
        var tokenA = login(client(port), ADMIN, PASSWORD);
        var tokenB = login(client(port), ADMIN_B_EMAIL, PASSWORD);
        var seededAppointmentA = "00000000-0000-7000-8000-000000000801";

        // practice A has a seeded clinic day; practice B has none
        assertThat((List<Object>) client(port).get().uri("/api/v1/appointments")
            .header("Authorization", "Bearer " + tokenA).retrieve().body(List.class)).isNotEmpty();
        assertThat((List<Object>) client(port).get().uri("/api/v1/appointments")
            .header("Authorization", "Bearer " + tokenB).retrieve().body(List.class)).isEmpty();

        var readB = client(port).get().uri("/api/v1/appointments/" + seededAppointmentA)
            .header("Authorization", "Bearer " + tokenB).retrieve().toEntity(String.class);
        assertThat(readB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var moveB = client(port).post().uri("/api/v1/appointments/" + seededAppointmentA + "/status")
            .header("Authorization", "Bearer " + tokenB).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("status", "cancelled")).retrieve().toEntity(String.class);
        assertThat(moveB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // the board and the day grid are views over the same table, so they are scoped the same way
        var boardB = client(port).get().uri("/api/v1/schedule/board")
            .header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
        assertThat((List<Object>) boardB.get("waiting")).isEmpty();
        assertThat((List<Object>) boardB.get("inExam")).isEmpty();
        assertThat((List<Object>) boardB.get("done")).isEmpty();

        var dayB = client(port).get().uri("/api/v1/schedule/day")
            .header("Authorization", "Bearer " + tokenB).retrieve().body(Map.class);
        assertThat((List<Object>) dayB.get("columns")).isEmpty();
        // B has no opening hours of its own and does not inherit A's
        assertThat(dayB.get("opens")).isNull();
    }

    // ---- part 11: the platform side ------------------------------------------------------------------

    /**
     * Part 04 kept `practices.status` away from the application role so that a bug in a feature service
     * could never suspend a clinic, and part 11 must not have quietly undone that to give the super admin
     * what it needs. The rights went to three definer functions instead; this proves the table itself is
     * still shut.
     */
    @Test
    void theApplicationRoleStillCannotChangeAPracticeDirectly() throws SQLException {
        try (var app = DriverManager.getConnection(postgres.getJdbcUrl(), "qvety_app", "qvety_app");
             var st = app.createStatement()) {

            assertThatThrownBy(() -> st.executeUpdate("""
                INSERT INTO practices (name, country, currency, locale, timezone)
                VALUES ('Sneaky Clinic', 'EG', 'EGP', 'ar-EG', 'Africa/Cairo')"""))
                .hasMessageContaining("permission denied for table practices");

            assertThatThrownBy(() -> st.executeUpdate("UPDATE practices SET status = 'suspended'"))
                .hasMessageContaining("permission denied for table practices");

            assertThatThrownBy(() -> st.executeUpdate("UPDATE practices SET closed_at = now()"))
                .hasMessageContaining("permission denied for table practices");
        }
    }

    /** The trail that says who suspended a clinic is worthless if the application can rewrite it. */
    @Test
    void thePlatformAuditTrailCannotBeRewritten() throws SQLException {
        try (var app = DriverManager.getConnection(postgres.getJdbcUrl(), "qvety_app", "qvety_app");
             var st = app.createStatement()) {
            st.executeUpdate("INSERT INTO platform_audit_log (action, target_type) VALUES ('probe', 'practice')");
            assertThatThrownBy(() -> st.executeUpdate("UPDATE platform_audit_log SET action = 'tampered'"))
                .hasMessageContaining("permission denied");
            assertThatThrownBy(() -> st.executeUpdate("DELETE FROM platform_audit_log"))
                .hasMessageContaining("permission denied");
        }
        // and not even the owner, because the guard trigger runs for everyone
        try (var owner = ownerConnection(postgres); var st = owner.createStatement()) {
            assertThatThrownBy(() -> st.executeUpdate("DELETE FROM platform_audit_log"))
                .hasMessageContaining("append-only");
        }
    }

    private static Connection ownerConnection(PostgreSQLContainer postgres) throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
