package com.qvety.practice;

import static com.qvety.ApiTestSupport.DESK;
import static com.qvety.ApiTestSupport.PASSWORD;
import static com.qvety.ApiTestSupport.PRACTICE_ID;
import static com.qvety.ApiTestSupport.client;
import static com.qvety.ApiTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.qvety.TestcontainersConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/** Part 02 (updated in part 03): the practice comes from the token, never from a header. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class PracticeIT {

    @LocalServerPort
    int port;

    @Test
    void ownPracticeIsReturnedFromTheToken() {
        var token = login(client(port), DESK, PASSWORD);
        var response = client(port).get().uri("/api/v1/practice")
            .header("Authorization", "Bearer " + token)
            .header("X-Practice-Id", UUID.randomUUID().toString())   // ignored: part 02 hack is gone
            .retrieve().toEntity(PracticeDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(UUID.fromString(PRACTICE_ID));
        assertThat(body.name()).isEqualTo("Neighborhood Vet");
        assertThat(body.country()).isEqualTo("EG");
        assertThat(body.currency()).isEqualTo("EGP");
        assertThat(body.status()).isEqualTo(PracticeStatus.trial);
    }

    @Test
    void withoutTokenIsUnauthorized() {
        var response = client(port).get().uri("/api/v1/practice").retrieve().toEntity(String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
