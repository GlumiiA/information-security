package ru.itmo.secureapi.auth;

import ru.itmo.secureapi.common.HtmlSanitizer;
import ru.itmo.secureapi.user.User;

public record UserResponse(Long id, String username, String role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), HtmlSanitizer.escape(user.getUsername()), user.getRole().name());
    }
}
