package com.qvety.auth;

import com.qvety.users.User;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/**
 * Two token shapes, one secret and one filter. A practice token carries practiceId, role and vet; a
 * platform token carries {@code platform: true} and no practiceId at all, because Qvety staff belong to
 * no practice. Both carry sv, so bumping a session version still kills every token of that account.
 * The platform token is issued by {@code platform.PlatformTokenService}: auth must not import platform.
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties props;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties props) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.props = props;
    }

    public String issue(User user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(props.ttl()))
            .claim("practiceId", user.getPracticeId().toString())
            .claim("role", user.getRole().name())
            .claim("vet", user.isVeterinarian())
            .claim("sv", user.getSessionVersion())
            .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Signature and expiry only. Active flag and session version are checked against the row in {@link JwtFilter}. */
    public Jwt verify(String token) throws JwtException {
        return decoder.decode(token);
    }
}
