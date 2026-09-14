package com.qvety;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Shared helpers: a client that never throws on 4xx/5xx, and login as a seeded dev user. */
public final class ApiTestSupport {

    public static final String PRACTICE_ID = "00000000-0000-7000-8000-000000000001";
    public static final String ADMIN = "admin@clinic.example.com";
    public static final String VET = "vet@clinic.example.com";
    public static final String TECH = "tech@clinic.example.com";
    public static final String DESK = "desk@clinic.example.com";
    public static final String DESK_PHONE = "+201000000104";
    public static final String PASSWORD = "password123";

    private ApiTestSupport() {}

    public static RestClient client(int port) {
        return RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultStatusHandler(status -> true, (req, res) -> { })
            .build();
    }

    @SuppressWarnings("unchecked")
    /** identifier: email or phone, exactly as a user would type it. */
    public static String login(RestClient client, String identifier, String password) {
        var body = client.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("identifier", identifier, "password", password))
            .retrieve().body(Map.class);
        if (body == null || body.get("token") == null) {
            throw new IllegalStateException("login failed for " + identifier + ": " + body);
        }
        return (String) body.get("token");
    }
}
