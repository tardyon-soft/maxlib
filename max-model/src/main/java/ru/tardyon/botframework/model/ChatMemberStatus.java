package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Membership status in a chat.
 */
public enum ChatMemberStatus {
    CREATOR("creator"),
    ADMINISTRATOR("administrator"),
    MEMBER("member"),
    RESTRICTED("restricted"),
    LEFT("left"),
    BANNED("banned"),
    UNKNOWN("unknown");

    private final String value;

    ChatMemberStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ChatMemberStatus fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (ChatMemberStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
