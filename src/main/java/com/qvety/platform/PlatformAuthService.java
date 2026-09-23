package com.qvety.platform;

import com.qvety.auth.PlatformAuthentication;
import com.qvety.auth.PlatformPrincipal;
import com.qvety.tenant.SystemContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Login for Qvety staff, and the other half of {@link PlatformAuthentication}. Every read here runs in
 * system context: platform tables are outside the tenant boundary, so there is no practice to set, and
 * the transaction would otherwise be refused.
 */
@Service
public class PlatformAuthService implements PlatformAuthentication {

    /** Compared when the email is unknown so timing does not reveal whether an account exists. */
    private static final String DUMMY_HASH = "$2a$12$tRrkJgHmsapCuGvMgs1yT.kiQJf.56xF7z9UuX6b517QZJL7UY0E2";

    private final PlatformUserRepository users;
    private final PasswordEncoder passwords;
    private final PlatformTokenService tokens;

    public PlatformAuthService(PlatformUserRepository users, PasswordEncoder passwords, PlatformTokenService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public PlatformLoginResponse login(PlatformLoginRequest request) {
        var user = SystemContext.call(() -> users.findByEmailIgnoreCase(request.email().trim()).orElse(null));
        if (user == null || !user.isActive()) {
            passwords.matches(request.password(), DUMMY_HASH);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
        }
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
        }
        return new PlatformLoginResponse(tokens.issue(user), user.getFullName(), user.getEmail());
    }

    /**
     * Called on every platform request, so a deactivated staff member loses access at once. Not
     * transactional: system context has to be active before a transaction begins, so the repository call
     * inside opens its own.
     */
    @Override
    public Optional<PlatformPrincipal> verify(UUID userId, int sessionVersion) {
        return SystemContext.call(() -> users.findById(userId)
            .filter(PlatformUser::isActive)
            .filter(u -> u.getSessionVersion() == sessionVersion)
            .map(u -> new PlatformPrincipal(u.getId(), u.getFullName())));
    }
}
