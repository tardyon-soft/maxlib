package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * Upload type accepted by docs-shaped POST /uploads.
 */
public enum UploadType {
    IMAGE,
    VIDEO,
    AUDIO,
    FILE;

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
