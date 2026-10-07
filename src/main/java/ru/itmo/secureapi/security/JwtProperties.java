package ru.itmo.secureapi.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret base64-ключ HMAC (не короче 256 бит). Берётся из переменной окружения JWT_SECRET;
 *               если не задан — при старте генерируется случайный ключ.
 * @param ttl    время жизни access-токена
 * @param issuer значение claim "iss"
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {
}
