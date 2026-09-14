package com.qvety.auth;

import com.qvety.users.UserDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "me")
public class MeController {

    private final AuthService service;

    public MeController(AuthService service) {
        this.service = service;
    }

    @GetMapping
    public UserDto me() {
        return service.me();
    }

    @PatchMapping
    public UserDto updateMe(@Valid @RequestBody UpdateMeRequest request) {
        return service.updateMe(request);
    }

    @PostMapping("/password")
    public LoginResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return service.changePassword(request);
    }
}
