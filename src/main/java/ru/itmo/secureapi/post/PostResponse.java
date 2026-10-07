package ru.itmo.secureapi.post;

import java.time.Instant;
import ru.itmo.secureapi.common.HtmlSanitizer;

/** Все строковые поля, пришедшие от пользователя, экранируются перед отдачей клиенту (защита от XSS). */
public record PostResponse(Long id, String title, String content, String author, Instant createdAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                HtmlSanitizer.escape(post.getTitle()),
                HtmlSanitizer.escape(post.getContent()),
                HtmlSanitizer.escape(post.getAuthor().getUsername()),
                post.getCreatedAt());
    }
}
