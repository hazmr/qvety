package com.qvety.common;

import org.springframework.http.HttpStatus;

/**
 * A business rule said no. code is a stable key in messages_*.properties and goes to the client as-is;
 * the handler resolves the localized text.
 */
public class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String field;

    public DomainException(HttpStatus status, String code) {
        this(status, code, null);
    }

    public DomainException(HttpStatus status, String code, String field) {
        super(code);
        this.status = status;
        this.code = code;
        this.field = field;
    }

    public static DomainException badRequest(String code) {
        return new DomainException(HttpStatus.BAD_REQUEST, code);
    }

    /** A rule that belongs to one request field; the handler reports it under that field like bean validation. */
    public static DomainException badRequest(String code, String field) {
        return new DomainException(HttpStatus.BAD_REQUEST, code, field);
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

    /** Request field the rule applies to, or null. */
    public String field() {
        return field;
    }
}
