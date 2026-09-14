package com.qvety.auth;

import com.qvety.users.User;
import com.qvety.users.UserDto;
import com.qvety.users.UserMapper;
import com.qvety.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    /** Bcrypt hash of a throwaway password, compared when the email is unknown so timing does not reveal existence. */
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

    public LoginResponse login(LoginRequest request, String ip) {
        long wait = limiter.retryAfterSeconds(request.email(), ip);
        if (wait > 0) {
            throw new TooManyLoginAttemptsException(wait);
        }
        var user = users.findByEmailIgnoreCase(request.email().trim()).stream()
            .filter(User::isActive)
            .findFirst()
            .orElse(null);
        var hash = user == null ? DUMMY_HASH : user.getPasswordHash();
        if (user == null || !passwords.matches(request.password(), hash)) {
            limiter.recordFailure(request.email(), ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
        }
        limiter.recordSuccess(request.email());
        return new LoginResponse(jwt.issue(user), mapper.toDto(user));
    }

    @Transactional(readOnly = true)
    public UserDto me() {
        return mapper.toDto(load());
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
        return new LoginResponse(jwt.issue(user), mapper.toDto(user));
    }

    private User load() {
        return users.findById(currentUser.userId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
