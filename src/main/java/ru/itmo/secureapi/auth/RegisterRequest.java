package ru.itmo.secureapi.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_.-]{3,32}$",
                message = "3-32 characters: latin letters, digits, '_', '.', '-'")
        String username,

        // bcrypt учитывает только первые 72 байта пароля, поэтому ограничиваем длину
        @NotBlank @Size(min = 8, max = 72) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", password=***]";
    }
}
