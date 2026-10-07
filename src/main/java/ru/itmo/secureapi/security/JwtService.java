package ru.itmo.secureapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.itmo.secureapi.user.User;

@Service
public final class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration ttl;
    private final String issuer;

    public JwtService(JwtProperties properties) {
        if (StringUtils.hasText(properties.secret())) {
            // Keys.hmacShaKeyFor бросает исключение, если ключ короче 256 бит
            this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        } else {
            log.warn("JWT_SECRET is not set: generated a random key, tokens will be invalid after restart");
            this.key = Jwts.SIG.HS256.key().build();
        }
        this.ttl = properties.ttl();
        this.issuer = properties.issuer();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Проверяет подпись, срок действия и издателя. Неподписанные токены (alg=none) и токены,
     * подписанные другим ключом, отклоняются библиотекой.
     */
    public Optional<String> validateAndGetUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            // в лог пишем только тип ошибки: сообщение может содержать данные из токена (CRLF-инъекция в логи)
            log.debug("Rejected JWT: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public Duration getTtl() {
        return ttl;
    }
}
