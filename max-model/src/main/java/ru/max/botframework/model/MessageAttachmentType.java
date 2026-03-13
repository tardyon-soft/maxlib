package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported attachment kinds.
 */
public enum MessageAttachmentType {
    INLINE_KEYBOARD("inline_keyboard"),
    PHOTO("photo"),
    VIDEO("video"),
    AUDIO("audio"),
    DOCUMENT("document"),
    FILE("file"),
    UNKNOWN("unknown");

    private final String value;

    MessageAttachmentType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static MessageAttachmentType fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (MessageAttachmentType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
