package ru.itmo.secureapi.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 2000) String content) {
}
