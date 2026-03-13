package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * MAX chat DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Chat(
        ChatId id,
        ChatType type,
        String title,
        String username,
        String description
) {
    public Chat {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
    }
}
