package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Chat admin permission values supported by MAX API.
 */
public enum ChatAdminPermission {
    SEND_MESSAGES("send_messages"),
    DELETE_MESSAGES("delete_messages"),
    PIN_MESSAGES("pin_messages"),
    BAN_USERS("ban_users"),
    INVITE_USERS("invite_users"),
    CHANGE_CHAT_INFO("change_chat_info"),
    MANAGE_CALLS("manage_calls"),
    MANAGE_TOPICS("manage_topics"),
    EDIT_LINK("edit_link"),
    UNKNOWN("unknown");

    private final String value;

    ChatAdminPermission(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ChatAdminPermission fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (ChatAdminPermission permission : values()) {
            if (permission.value.equalsIgnoreCase(value)) {
                return permission;
            }
        }
        return UNKNOWN;
    }
}
