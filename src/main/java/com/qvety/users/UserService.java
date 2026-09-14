package com.qvety.users;

import com.qvety.auth.CurrentUser;
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
        var email = request.email().trim();
        if (users.findByPracticeIdAndEmailIgnoreCase(practiceId, email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email_taken");
        }
        var user = new User();
        user.setPracticeId(practiceId);
        user.setEmail(email);
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setVeterinarian(request.veterinarian());
        user.setLicenseNumber(request.licenseNumber());
        user.setPhone(request.phone());
        user.setPasswordHash(passwords.encode(request.temporaryPassword()));
        user.setMustChangePassword(true);
        return mapper.toDto(users.saveAndFlush(user));
    }

    @Transactional
    public UserDto update(UUID id, UserUpdateRequest request) {
        var user = load(id);
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

    private User load(UUID id) {
        return users.findByIdAndPracticeId(id, currentUser.practiceId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user_not_found"));
    }
}
