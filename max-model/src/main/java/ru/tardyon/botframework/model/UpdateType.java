package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * High-level update types supported by MAX bot framework pipeline.
 */
public enum UpdateType {
    MESSAGE("message"),
    CALLBACK("callback"),
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
