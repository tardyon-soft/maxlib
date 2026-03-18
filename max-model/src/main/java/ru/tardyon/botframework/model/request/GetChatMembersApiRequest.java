package ru.tardyon.botframework.model.request;

import java.util.List;

/**
 * Docs-shaped request for GET /chats/{chatId}/members.
 */
public record GetChatMembersApiRequest(
        List<Long> userIds,
        Long marker,
        Integer count
) {
    public GetChatMembersApiRequest {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
        if (count != null && (count < 1 || count > 100)) {
            throw new IllegalArgumentException("count must be between 1 and 100");
        }
    }

    public static GetChatMembersApiRequest defaults() {
        return new GetChatMembersApiRequest(List.of(), null, null);
    }
}
