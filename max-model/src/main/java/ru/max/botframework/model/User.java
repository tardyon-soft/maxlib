package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * MAX user DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record User(
        String id,
        String username,
        String firstName,
        String lastName,
        String displayName,
        Boolean bot,
        String languageCode
) {
    public User {
        Objects.requireNonNull(id, "id");
    }
}
