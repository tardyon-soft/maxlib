package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Chat admin permission values supported by MAX API.
 */
public enum ChatAdminPermission {
    READ_ALL_MESSAGES("read_all_messages"),
    ADD_REMOVE_MEMBERS("add_remove_members"),
    ADD_ADMINS("add_admins"),
    PIN_MESSAGE("pin_message"),
    WRITE("write"),
    CAN_CALL("can_call"),
    POST_EDIT_DELETE_MESSAGE("post_edit_delete_message"),
    EDIT_MESSAGE("edit_message"),
    DELETE_MESSAGE("delete_message"),
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
