package com.qvety.users;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody UserCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public UserDto deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }

    @PostMapping("/{id}/reset-password")
    public UserDto resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        return service.resetPassword(id, request);
    }
}
