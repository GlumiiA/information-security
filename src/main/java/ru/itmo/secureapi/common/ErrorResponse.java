package ru.itmo.secureapi.common;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(int status, String error, Map<String, String> details, Instant timestamp) {

    public static ErrorResponse of(int status, String error) {
        return new ErrorResponse(status, error, Map.of(), Instant.now());
    }

    public static ErrorResponse of(int status, String error, Map<String, String> details) {
        return new ErrorResponse(status, error, Map.copyOf(details), Instant.now());
    }
}
