package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * High-level update types supported by MAX bot framework pipeline.
 */
public enum UpdateType {
    MESSAGE("message"),
    CALLBACK("callback"),
    MESSAGE_EDITED("message_edited"),
    MESSAGE_REMOVED("message_removed"),
    BOT_ADDED("bot_added"),
    BOT_REMOVED("bot_removed"),
    BOT_STARTED("bot_started"),
    BOT_STOPPED("bot_stopped"),
    USER_ADDED("user_added"),
    USER_REMOVED("user_removed"),
    CHAT_TITLE_CHANGED("chat_title_changed"),
    MESSAGE_CHAT_CREATED("message_chat_created"),
    MESSAGE_CONSTRUCTION_REQUEST("message_construction_request"),
    MESSAGE_CONSTRUCTED("message_constructed"),
    DIALOG_MUTED("dialog_muted"),
    DIALOG_UNMUTED("dialog_unmuted"),
    DIALOG_CLEARED("dialog_cleared"),
    DIALOG_REMOVED("dialog_removed"),
    CHAT_MEMBER("chat_member"),
    UNKNOWN("unknown");

    private final String value;

    UpdateType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static UpdateType fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (UpdateType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
