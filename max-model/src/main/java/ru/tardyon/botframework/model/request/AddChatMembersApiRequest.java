package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Docs-shaped request for POST /chats/{chatId}/members.
 */
public record AddChatMembersApiRequest(
        @JsonProperty("user_ids") List<Long> userIds
) {
    public AddChatMembersApiRequest {
        Objects.requireNonNull(userIds, "userIds");
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("userIds must not be empty");
        }
        userIds = List.copyOf(userIds);
    }
}
