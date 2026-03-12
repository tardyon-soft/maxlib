package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Objects;

/**
 * Callback event payload DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Callback(
        CallbackId callbackId,
        String data,
        User from,
        Message message,
        Instant createdAt
) {
    public Callback {
        Objects.requireNonNull(callbackId, "callbackId");
    }
}
