package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * DTO returned by get-bot-info-like endpoints.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BotInfo(
        String id,
        String username,
        String displayName,
        String about,
        String avatarUrl
) {
    public BotInfo {
        Objects.requireNonNull(id, "id");
    }
}
