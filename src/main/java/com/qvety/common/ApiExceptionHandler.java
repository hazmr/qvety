package com.qvety.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Turns validation failures and ResponseStatusException reasons into {@link ApiError}, with the
 * message resolved from messages_*.properties for the caller's Accept-Language. Part 06 adds the
 * DomainException and optimistic-lock cases.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private final MessageSource messages;

    public ApiExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (var fe : e.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());   // already interpolated via MessageSource
        }
        return ResponseEntity.badRequest().body(new ApiError("validation_failed", text("validation.failed"), fields));
    }

    /** Unparseable JSON or a null into a primitive: 400 with a stable code, no parser internals leaked. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ApiError("malformed_request", text("malformed_request"), null));
    }

    /** reason is used as the code; the message comes from the bundle when a key exists. */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException e) {
        var code = e.getReason() == null ? HttpStatus.valueOf(e.getStatusCode().value()).name().toLowerCase() : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(new ApiError(code, text(code), null));
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }
}
