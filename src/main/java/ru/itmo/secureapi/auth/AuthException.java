package ru.itmo.secureapi.auth;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
