package com.qvety.reference;

import com.qvety.common.DomainException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives a new practice its country's opening rooms, appointment types, and services from
 * {@code catalog/<country>.sql}. The file is data: a clinic's starter lists change without a Java release.
 *
 * Part 11 calls this inside the practice-creation transaction, before the practice has any user, so the
 * caller is responsible for the tenant session variable (a system context with the new practice id).
 * A country with no catalog file fails creation rather than leaving a practice with empty lists.
 */
@Component
public class StarterCatalogSeeder {

    private final NamedParameterJdbcTemplate jdbc;

    public StarterCatalogSeeder(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @param country ISO 3166-1 alpha-2, as stored on the practice row. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void seed(UUID practiceId, String country) {
        jdbc.update(read(country), new MapSqlParameterSource("practice_id", practiceId));
    }

    private static String read(String country) {
        var resource = new ClassPathResource("catalog/" + country.toLowerCase(Locale.ROOT) + ".sql");
        if (!resource.exists()) {
            throw DomainException.badRequest("practice.country_unsupported", "country");
        }
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("catalog for " + country + " could not be read", e);
        }
    }
}
