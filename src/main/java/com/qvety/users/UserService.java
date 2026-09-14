package com.qvety.users;

import com.qvety.auth.CurrentUser;
import com.qvety.common.PhoneNormalizer;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Admin-only user management. Every method scopes by the caller's practice; RLS (part 04) adds the second wall. */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class UserService {

    private final UserRepository users;
    private final UserMapper mapper;
    private final PasswordEncoder passwords;
    private final CurrentUser currentUser;

    public UserService(UserRepository users, UserMapper mapper, PasswordEncoder passwords, CurrentUser currentUser) {
        this.users = users;
        this.mapper = mapper;
        this.passwords = passwords;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findByPracticeIdOrderByFullName(currentUser.practiceId()).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto get(UUID id) {
        return mapper.toDto(load(id));
    }

    @Transactional
    public UserDto create(UserCreateRequest request) {
        var practiceId = currentUser.practiceId();
        var user = new User();
        user.setPracticeId(practiceId);
        applyContact(user, request.phone(), request.email());
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setVeterinarian(request.veterinarian());
        user.setLicenseNumber(request.licenseNumber());
        user.setPasswordHash(passwords.encode(request.temporaryPassword()));
        user.setMustChangePassword(true);
        return mapper.toDto(users.saveAndFlush(user));
    }

    @Transactional
    public UserDto update(UUID id, UserUpdateRequest request) {
        var user = load(id);
        applyContact(user, request.phone(), request.email());
        mapper.updateFromRequest(request, user);
        return mapper.toDto(users.saveAndFlush(user));
    }

    /** Stays in history; every token dies now. */
    @Transactional
    public UserDto deactivate(UUID id) {
        var user = load(id);
        if (user.getId().equals(currentUser.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "cannot_deactivate_self");
        }
        user.setActive(false);
        user.revokeSessions();
        return mapper.toDto(user);
    }

    /** Temporary password, forced change, every existing token dies. */
    @Transactional
    public UserDto resetPassword(UUID id, ResetPasswordRequest request) {
        var user = load(id);
        user.setPasswordHash(passwords.encode(request.temporaryPassword()));
        user.setMustChangePassword(true);
        user.revokeSessions();
        return mapper.toDto(user);
    }

    /** Phone to E.164 (must parse), email lowercased or null; both unique inside the practice. */
    private void applyContact(User user, String rawPhone, String rawEmail) {
        var practiceId = currentUser.practiceId();
        var phone = PhoneNormalizer.toE164(rawPhone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone_invalid"));
        var email = rawEmail == null || rawEmail.isBlank() ? null : rawEmail.trim().toLowerCase();
        users.findByPracticeIdAndPhone(practiceId, phone)
            .filter(other -> !other.getId().equals(user.getId()))
            .ifPresent(other -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "phone_taken"); });
        if (email != null) {
            users.findByPracticeIdAndEmailIgnoreCase(practiceId, email)
                .filter(other -> !other.getId().equals(user.getId()))
                .ifPresent(other -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "email_taken"); });
        }
        user.setPhone(phone);
        user.setEmail(email);
    }

    private User load(UUID id) {
        return users.findByIdAndPracticeId(id, currentUser.practiceId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user_not_found"));
    }
}
