package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported entity types for message text markup.
 */
public enum MessageEntityType {
    MENTION("mention"),
    HASHTAG("hashtag"),
    BOT_COMMAND("bot_command"),
    URL("url"),
    EMAIL("email"),
    PHONE("phone"),
    BOLD("bold"),
    ITALIC("italic"),
    CODE("code"),
    TEXT_LINK("text_link"),
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
