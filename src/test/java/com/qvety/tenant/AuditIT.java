package com.qvety.tenant;

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

/** Part 04: the audit log is trigger-written, readable by admins, and never shows secrets. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class AuditIT {

    @LocalServerPort
    int port;

    @Test
    @SuppressWarnings("unchecked")
    void userRenameIsAuditedWithActorAndWithoutSecrets() {
        var admin = login(client(port), ADMIN, PASSWORD);
        var me = client(port).get().uri("/api/v1/me").header("Authorization", "Bearer " + admin)
            .retrieve().body(Map.class);
        var adminId = (String) me.get("id");

        var created = client(port).post().uri("/api/v1/users").header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", "audit-case@clinic.example.com", "temporaryPassword", "temporary-1",
                "fullName", "Before Name", "role", "technician", "veterinarian", false))
            .retrieve().body(Map.class);
        var id = (String) created.get("id");

        client(port).put().uri("/api/v1/users/" + id).header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("fullName", "After Name", "role", "technician", "veterinarian", false))
            .retrieve().toBodilessEntity();

        var page = client(port).get().uri("/api/v1/audit?table=users&rowId=" + id)
            .header("Authorization", "Bearer " + admin).retrieve().body(Map.class);
        var rows = (List<Map<String, Object>>) page.get("content");
        assertThat(rows).hasSize(2);   // newest first: update, then insert

        var update = rows.get(0);
        assertThat(update).containsEntry("action", "update").containsEntry("userId", adminId);
        var before = (Map<String, Object>) update.get("before");
        var after = (Map<String, Object>) update.get("after");
        assertThat(before).containsEntry("full_name", "Before Name").doesNotContainKey("password_hash");
        assertThat(after).containsEntry("full_name", "After Name").doesNotContainKey("password_hash");

        var insert = rows.get(1);
        assertThat(insert).containsEntry("action", "insert");
        assertThat((Map<String, Object>) insert.get("after")).doesNotContainKey("password_hash");
    }

    @Test
    void frontDeskCannotReadAudit() {
        var desk = login(client(port), DESK, PASSWORD);
        var r = client(port).get().uri("/api/v1/audit").header("Authorization", "Bearer " + desk)
            .retrieve().toEntity(String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
