package com.qvety.practice;

import static org.assertj.core.api.Assertions.assertThat;

import com.qvety.TestcontainersConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * Part 02: Flyway applies V1 and the seed on a real Postgres 18; the practice endpoint reads it back.
 * The X-Practice-Id header is scaffolding and disappears in part 03.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class PracticeIT {

    static final String SEEDED_PRACTICE_ID = "00000000-0000-7000-8000-000000000001";

    @LocalServerPort
    int port;

    RestClient client() {
        return RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultStatusHandler(status -> true, (req, res) -> { })
            .build();
    }

    @Test
    void seededPracticeIsReturned() {
        var response = client().get().uri("/api/v1/practice")
            .header("X-Practice-Id", SEEDED_PRACTICE_ID)
            .retrieve().toEntity(PracticeDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(UUID.fromString(SEEDED_PRACTICE_ID));
        assertThat(body.name()).isEqualTo("Neighborhood Vet");
        assertThat(body.country()).isEqualTo("EG");
        assertThat(body.currency()).isEqualTo("EGP");
        assertThat(body.status()).isEqualTo(PracticeStatus.trial);
    }

    @Test
    void unknownPracticeIsNotFound() {
        var response = client().get().uri("/api/v1/practice")
            .header("X-Practice-Id", UUID.randomUUID().toString())
            .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void missingHeaderIsBadRequest() {
        var response = client().get().uri("/api/v1/practice")
            .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
