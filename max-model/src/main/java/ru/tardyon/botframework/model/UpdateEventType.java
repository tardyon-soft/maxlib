package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Raw update type filters supported by getUpdates API.
 */
public enum UpdateEventType {
    BOT_ADDED("bot_added"),
    BOT_REMOVED("bot_removed"),
    BOT_STARTED("bot_started"),
    BOT_STOPPED("bot_stopped"),
    MESSAGE_CREATED("message_created"),
    MESSAGE_CALLBACK("message_callback"),
    MESSAGE_EDITED("message_edited"),
    MESSAGE_REMOVED("message_removed"),
    USER_ADDED("user_added"),
    USER_REMOVED("user_removed"),
    CHAT_TITLE_CHANGED("chat_title_changed"),
    MESSAGE_CHAT_CREATED("message_chat_created"),
    MESSAGE_CONSTRUCTION_REQUEST("message_construction_request"),
    MESSAGE_CONSTRUCTED("message_constructed"),
    CHAT_MEMBER("chat_member"),
    DIALOG_MUTED("dialog_muted"),
    DIALOG_UNMUTED("dialog_unmuted"),
    DIALOG_CLEARED("dialog_cleared"),
    DIALOG_REMOVED("dialog_removed"),
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
