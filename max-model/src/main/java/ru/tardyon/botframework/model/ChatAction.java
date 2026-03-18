package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Chat action values supported by MAX API.
 */
public enum ChatAction {
    TYPING("typing_on"),
    SENDING_PHOTO("sending_photo"),
    SENDING_VIDEO("sending_video"),
    SENDING_AUDIO("sending_audio"),
    SENDING_FILE("sending_file"),
    SENDING_STICKER("sending_sticker"),
    SENDING_LOCATION("sending_location"),
    RECORDING_VOICE("recording_voice"),
    RECORDING_VIDEO_NOTE("recording_video_note"),
    CHOOSING_CONTACT("choosing_contact"),
    UNKNOWN("unknown");

    private final String value;

    ChatAction(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ChatAction fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        if ("typing".equalsIgnoreCase(value)) {
            return TYPING;
        }
        for (ChatAction action : values()) {
            if (action.value.equalsIgnoreCase(value)) {
                return action;
            }
        }
        return UNKNOWN;
    }
}
