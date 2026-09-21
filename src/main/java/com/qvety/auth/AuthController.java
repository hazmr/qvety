package com.qvety.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.qvety.common.ApiError;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "auth")
public class AuthController {

    private final AuthService service;
    private final MessageSource messages;

    public AuthController(AuthService service, MessageSource messages) {
        this.service = service;
        this.messages = messages;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return service.login(request, clientIp(http));
    }

    /** Revokes the caller's session on the server; the token in hand stops working at once. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        service.logout();
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    ResponseEntity<ApiError> tooMany(TooManyLoginAttemptsException e) {
        var message = messages.getMessage("too_many_attempts", null, "too_many_attempts", LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", Long.toString(e.retryAfterSeconds()))
            .body(new ApiError("too_many_attempts", message, null));
    }

    /** Behind Caddy (part 16) the real address is in X-Forwarded-For; locally it is the socket address. */
    private static String clientIp(HttpServletRequest http) {
        var forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
