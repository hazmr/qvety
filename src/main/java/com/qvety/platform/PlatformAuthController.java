package com.qvety.platform;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The only platform endpoint open without a token. */
@RestController
@RequestMapping(value = "/api/platform/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "platform")
public class PlatformAuthController {

    private final PlatformAuthService service;

    public PlatformAuthController(PlatformAuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public PlatformLoginResponse platformLogin(@Valid @RequestBody PlatformLoginRequest request) {
        return service.login(request);
    }
}
