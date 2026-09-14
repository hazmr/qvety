package com.qvety.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code qvety.jwt.secret} comes from the environment (QVETY_JWT_SECRET). Outside the local profile the
 * app refuses to start without it; see {@link JwtConfig}.
 */
@ConfigurationProperties(prefix = "qvety.jwt")
public record JwtProperties(String secret, Duration ttl) {

    public JwtProperties {
        if (ttl == null) {
            ttl = Duration.ofHours(12);
        }
    }
}
