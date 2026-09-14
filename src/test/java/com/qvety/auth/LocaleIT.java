package com.qvety.auth;

import static com.qvety.ApiTestSupport.ADMIN;
import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.PASSWORD;
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

/** Part 05: per-user locale, and backend messages in the caller's language. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class LocaleIT {

    @LocalServerPort
    int port;

    @Test
    void localeDefaultsToArabicAndCanBeChanged() {
        var token = login(client(port), DESK, PASSWORD);
        var me = client(port).get().uri("/api/v1/me").header("Authorization", "Bearer " + token).retrieve().body(Map.class);
        assertThat(me).containsEntry("locale", "ar-EG");

        var patched = client(port).patch().uri("/api/v1/me").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("locale", "en-EG")).retrieve().toEntity(Map.class);
        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patched.getBody()).containsEntry("locale", "en-EG");

        var again = client(port).get().uri("/api/v1/me").header("Authorization", "Bearer " + token).retrieve().body(Map.class);
        assertThat(again).containsEntry("locale", "en-EG");

        var bad = client(port).patch().uri("/api/v1/me").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("locale", "fr-FR")).retrieve().toEntity(Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bad.getBody()).containsEntry("code", "validation_failed");

        client(port).patch().uri("/api/v1/me").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("locale", "ar-EG")).retrieve().toBodilessEntity();
    }

    @Test
    @SuppressWarnings("unchecked")
    void validationMessagesFollowAcceptLanguage() {
        var admin = login(client(port), ADMIN, PASSWORD);
        var body = Map.of("phone", "", "temporaryPassword", "short", "fullName", "", "role", "technician", "veterinarian", false);

        var ar = client(port).post().uri("/api/v1/users").header("Authorization", "Bearer " + admin)
            .header("Accept-Language", "ar-EG").contentType(MediaType.APPLICATION_JSON).body(body)
            .retrieve().toEntity(Map.class);
        assertThat(ar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ar.getBody()).containsEntry("message", "بعض الحقول غير صحيحة.");
        var fieldsAr = (Map<String, String>) ar.getBody().get("fields");
        assertThat(fieldsAr).containsEntry("fullName", "مطلوب.");
        assertThat(fieldsAr.get("temporaryPassword")).contains("10");

        var en = client(port).post().uri("/api/v1/users").header("Authorization", "Bearer " + admin)
            .header("Accept-Language", "en-EG").contentType(MediaType.APPLICATION_JSON).body(body)
            .retrieve().toEntity(Map.class);
        assertThat(en.getBody()).containsEntry("message", "Some fields are invalid.");
        assertThat((Map<String, String>) en.getBody().get("fields")).containsEntry("fullName", "Required.");
    }

    @Test
    void domainErrorsAreLocalized() {
        var ar = client(port).post().uri("/api/v1/auth/login").header("Accept-Language", "ar-EG")
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("identifier", DESK, "password", "wrong"))
            .retrieve().toEntity(Map.class);
        assertThat(ar.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ar.getBody()).containsEntry("code", "invalid_credentials")
            .containsEntry("message", "رقم الهاتف أو البريد أو كلمة المرور غير صحيحة.");

        var en = client(port).post().uri("/api/v1/auth/login").header("Accept-Language", "en")
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("identifier", DESK, "password", "wrong"))
            .retrieve().toEntity(Map.class);
        assertThat(en.getBody()).containsEntry("message", "Wrong phone, email, or password.");
    }
}
