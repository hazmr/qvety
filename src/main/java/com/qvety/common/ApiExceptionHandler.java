package com.qvety.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.persistence.OptimisticLockException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Turns validation failures and ResponseStatusException reasons into {@link ApiError}, with the
 * message resolved from messages_*.properties for the caller's Accept-Language.
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

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException e) {
        var fields = e.field() == null ? null : Map.of(e.field(), text(e.code()));
        return ResponseEntity.status(e.status()).body(new ApiError(e.code(), text(e.code()), fields));
    }

    /** Two edits raced (two tabs, two people): the second one loses and reloads. */
    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ApiError> staleUpdate(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("stale_update", text("stale_update"), null));
    }

    /** Sorting by a property the entity does not have. */
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ApiError> badSort(PropertyReferenceException e) {
        return ResponseEntity.badRequest().body(new ApiError("bad_sort", text("bad_sort"), null));
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
