package ru.itmo.secureapi.common;

import org.springframework.web.util.HtmlUtils;

/** Экранирование пользовательских данных встроенными средствами Spring (HtmlUtils). */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    public static String escape(String value) {
        return value == null ? null : HtmlUtils.htmlEscape(value, "UTF-8");
    }
}
