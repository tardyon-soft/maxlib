package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Text formatting mode for outgoing messages.
 */
public enum TextFormat {
    PLAIN("plain"),
    MARKDOWN("markdown"),
    HTML("html"),
    UNKNOWN("unknown");

    private final String value;

    TextFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static TextFormat fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (TextFormat format : values()) {
            if (format.value.equalsIgnoreCase(value)) {
                return format;
            }
        }
        return UNKNOWN;
    }
}
