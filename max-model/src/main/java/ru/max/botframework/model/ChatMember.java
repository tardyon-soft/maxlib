package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Objects;

/**
 * Chat membership DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMember(
        Chat chat,
        User user,
        ChatMemberStatus status,
        Instant joinedAt,
        Instant updatedAt
) {
    public ChatMember {
        Objects.requireNonNull(chat, "chat");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(status, "status");
    }
}
