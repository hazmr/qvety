package com.qvety.auth;

import com.qvety.users.User;
import com.qvety.users.UserDto;
import com.qvety.users.UserMapper;
import com.qvety.common.PhoneNormalizer;
import com.qvety.tenant.SystemContext;
import com.qvety.users.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    /** Bcrypt hash of a throwaway password, compared when the identifier is unknown so timing does not reveal existence. */
    private static final String DUMMY_HASH = "$2a$12$tRrkJgHmsapCuGvMgs1yT.kiQJf.56xF7z9UuX6b517QZJL7UY0E2";

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final LoginRateLimiter limiter;
    private final UserMapper mapper;
    private final CurrentUser currentUser;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtService jwt,
                       LoginRateLimiter limiter, UserMapper mapper, CurrentUser currentUser) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.limiter = limiter;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    /**
     * No tenant before login: the lookup runs in SystemContext through the definer function.
     * The identifier is an email (contains '@') or a phone normalized to E.164; a phone that does
     * not parse is treated as unknown (401), never as a validation error, so nothing is revealed.
     *
     * The same identifier may exist at several practices (unique per practice, not globally). Every
     * active row is tried, in practice-id order so the result never depends on row order. One match:
     * token. Several: the practice list, and the client calls again with practiceId. None: 401.
     */
    public LoginResponse login(LoginRequest request, String ip) {
        var identifier = normalizeIdentifier(request.identifier());
        long wait = limiter.retryAfterSeconds(identifier, ip);
        if (wait > 0) {
            throw new TooManyLoginAttemptsException(wait);
        }
        var candidates = identifier.isEmpty() ? List.<User>of() : SystemContext.call(() -> users.findForLogin(identifier).stream()
            .filter(User::isActive)
            .filter(u -> request.practiceId() == null || u.getPracticeId().equals(request.practiceId()))
            .sorted(Comparator.comparing(User::getPracticeId))
            .toList());
        // every hash is checked, and an unknown identifier still costs one bcrypt, so timing does not tell rows apart
        var matches = candidates.stream().filter(u -> passwords.matches(request.password(), u.getPasswordHash())).toList();
        if (candidates.isEmpty()) {
            passwords.matches(request.password(), DUMMY_HASH);
        }
        if (matches.isEmpty()) {
            limiter.recordFailure(identifier, ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
        }
        limiter.recordSuccess(identifier);
        if (matches.size() == 1) {
            var user = matches.getFirst();
            return LoginResponse.loggedIn(jwt.issue(user), mapper.toDto(user));
        }
        var ids = matches.stream().map(User::getPracticeId).toArray(UUID[]::new);
        var choices = SystemContext.call(() -> users.findLoginPractices(ids)).stream()
            .map(p -> new PracticeChoice(p.getId(), p.getName()))
            .toList();
        return LoginResponse.choose(choices);
    }

    @Transactional(readOnly = true)
    public UserDto me() {
        return mapper.toDto(load());
    }

    @Transactional
    public UserDto updateMe(UpdateMeRequest request) {
        var user = load();
        user.setLocale(request.locale());
        return mapper.toDto(user);
    }

    /** Bumps session_version: every other token dies, the caller gets a fresh one. */
    @Transactional
    public LoginResponse changePassword(ChangePasswordRequest request) {
        var user = load();
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "current_password_wrong");
        }
        user.setPasswordHash(passwords.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.revokeSessions();
        return LoginResponse.loggedIn(jwt.issue(user), mapper.toDto(user));
    }

    /** Email lowercased, phone as E.164, or "" when it is neither (all unparseable identifiers share one bucket; the IP bucket still counts). */
    private static String normalizeIdentifier(String raw) {
        var trimmed = raw == null ? "" : raw.trim();
        if (PhoneNormalizer.looksLikeEmail(trimmed)) {
            return trimmed.toLowerCase();
        }
        return PhoneNormalizer.toE164(trimmed).orElse("");
    }

    private User load() {
        return users.findById(currentUser.userId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
