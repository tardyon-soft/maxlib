package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Raw update type filters supported by getUpdates API.
 */
public enum UpdateEventType {
    BOT_ADDED("bot_added"),
    BOT_REMOVED("bot_removed"),
    MESSAGE_CREATED("message_created"),
    MESSAGE_CALLBACK("message_callback"),
    MESSAGE_EDITED("message_edited"),
    MESSAGE_REMOVED("message_removed"),
    CHAT_MEMBER("chat_member"),
    DIALOG_MUTED("dialog_muted"),
    DIALOG_UNMUTED("dialog_unmuted"),
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
