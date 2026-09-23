package com.qvety.platform;

import com.qvety.auth.JwtProperties;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * The super admin's token: {@code sub}, {@code platform: true}, {@code sv}, and no practiceId at all, so
 * nothing in it can be used to enter a tenant. Issued here rather than in {@code auth.JwtService} because
 * feature packages must not import platform; platform importing auth is the allowed direction.
 */
@Service
public class PlatformTokenService {

    /** The claim {@link com.qvety.auth.JwtFilter} branches on. */
    public static final String PLATFORM_CLAIM = "platform";

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public PlatformTokenService(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public String issue(PlatformUser user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(props.ttl()))
            .claim(PLATFORM_CLAIM, true)
            .claim("sv", user.getSessionVersion())
            .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
