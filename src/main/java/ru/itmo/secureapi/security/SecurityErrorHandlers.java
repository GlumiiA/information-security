package ru.itmo.secureapi.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Ответы 401/403 в JSON без раскрытия причин отказа. Тело — константа, данные запроса в ответ не попадают. */
public final class SecurityErrorHandlers {

    private static final byte[] UNAUTHORIZED_BODY =
            "{\"status\":401,\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FORBIDDEN_BODY =
            "{\"status\":403,\"error\":\"Forbidden\"}".getBytes(StandardCharsets.UTF_8);

    private SecurityErrorHandlers() {
    }

    public static AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) -> {
            response.setHeader("WWW-Authenticate", "Bearer");
            write(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_BODY);
        };
    }

    public static AccessDeniedHandler forbidden() {
        return (request, response, ex) -> write(response, HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_BODY);
    }

    private static void write(HttpServletResponse response, int status, byte[] body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }
}
