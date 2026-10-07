package ru.itmo.secureapi.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
