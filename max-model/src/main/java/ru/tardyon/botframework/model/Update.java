package ru.tardyon.botframework.model;

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
        ChatId chatId,
        User user,
        Boolean channel,
        Instant eventAt
) {
    public Update {
        Objects.requireNonNull(updateId, "updateId");
        Objects.requireNonNull(type, "type");
    }

    public Update(
            UpdateId updateId,
            UpdateType type,
            Message message,
            Callback callback,
            ChatMember chatMember,
            Instant eventAt
    ) {
        this(updateId, type, message, callback, chatMember, null, null, null, eventAt);
    }
}
