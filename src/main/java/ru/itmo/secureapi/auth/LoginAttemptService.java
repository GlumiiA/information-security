package ru.itmo.secureapi.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Защита от перебора паролей: после N неудачных попыток логин временно блокируется. */
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockDuration;
    private final Clock clock = Clock.systemUTC();

    public LoginAttemptService(
            @Value("${app.security.login.max-failures:5}") int maxFailures,
            @Value("${app.security.login.lock-duration:PT5M}") Duration lockDuration) {
        this.maxFailures = maxFailures;
        this.lockDuration = lockDuration;
    }

    public boolean isBlocked(String username) {
        Attempts current = attempts.get(key(username));
        return current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(clock.instant());
    }

    public void loginFailed(String username) {
        attempts.compute(key(username), (k, current) -> {
            int failures = (current == null || isExpired(current)) ? 1 : current.failures() + 1;
            Instant lockedUntil = failures >= maxFailures ? clock.instant().plus(lockDuration) : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    public void loginSucceeded(String username) {
        attempts.remove(key(username));
    }

    private boolean isExpired(Attempts current) {
        return current.lockedUntil() != null && !current.lockedUntil().isAfter(clock.instant());
    }

    private static String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
