package com.qvety.common;

import org.springframework.http.HttpStatus;

/**
 * A business rule said no. code is a stable key in messages_*.properties and goes to the client as-is;
 * the handler resolves the localized text.
 */
public class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public DomainException(HttpStatus status, String code) {
        super(code);
        this.status = status;
        this.code = code;
    }

    public static DomainException badRequest(String code) {
        return new DomainException(HttpStatus.BAD_REQUEST, code);
    }

    public static DomainException conflict(String code) {
        return new DomainException(HttpStatus.CONFLICT, code);
    }

    public static DomainException notFound(String code) {
        return new DomainException(HttpStatus.NOT_FOUND, code);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
