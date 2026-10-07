package ru.itmo.secureapi.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.secureapi.security.JwtService;
import ru.itmo.secureapi.user.Role;
import ru.itmo.secureapi.user.User;
import ru.itmo.secureapi.user.UserRepository;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    /** Хэш-«пустышка»: проверяется, когда пользователя нет, чтобы время ответа не выдавало существование логина. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginAttemptService loginAttempts) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new AuthException(HttpStatus.CONFLICT, "Username is already taken");
        }
        User user = users.save(new User(request.username(), passwordEncoder.encode(request.password()), Role.USER));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String username = request.username();
        if (loginAttempts.isBlocked(username)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts, try again later");
        }

        Optional<User> user = users.findByUsername(username);
        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (user.isEmpty() || !passwordMatches) {
            loginAttempts.loginFailed(username);
            throw new AuthException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        loginAttempts.loginSucceeded(username);
        return new TokenResponse(jwtService.generateToken(user.get()), "Bearer", jwtService.getTtl().toSeconds());
    }
}
