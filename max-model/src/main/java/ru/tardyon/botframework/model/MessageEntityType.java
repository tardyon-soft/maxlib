package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported MAX message markup element types.
 */
public enum MessageEntityType {
    STRONG("strong"),
    EMPHASIZED("emphasized"),
    MONOSPACED("monospaced"),
    LINK("link"),
    STRIKETHROUGH("strikethrough"),
    UNDERLINE("underline"),
    USER_MENTION("user_mention"),
    HEADING("heading"),
    HIGHLIGHTED("highlighted"),
    QUOTE("quote"),
    UNKNOWN("unknown");

    private final String value;

    MessageEntityType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static MessageEntityType fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (MessageEntityType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
