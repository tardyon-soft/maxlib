package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Raw update type filters supported by getUpdates API.
 */
public enum UpdateEventType {
    MESSAGE_CREATED("message_created"),
    MESSAGE_CALLBACK("message_callback"),
    CHAT_MEMBER("chat_member"),
    UNKNOWN("unknown");

    private final String value;

    UpdateEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static UpdateEventType fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (UpdateEventType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
