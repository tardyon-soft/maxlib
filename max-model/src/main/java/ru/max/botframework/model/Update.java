package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Objects;

/**
 * Normalized update DTO used across client/runtime contracts.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Update(
        UpdateId updateId,
        UpdateType type,
        Message message,
        Callback callback,
        ChatMember chatMember,
        Instant eventAt
) {
    public Update {
        Objects.requireNonNull(updateId, "updateId");
        Objects.requireNonNull(type, "type");
    }
}
