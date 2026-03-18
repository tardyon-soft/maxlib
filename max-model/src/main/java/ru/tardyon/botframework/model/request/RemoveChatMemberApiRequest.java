package ru.tardyon.botframework.model.request;

/**
 * Docs-shaped request for DELETE /chats/{chatId}/members.
 */
public record RemoveChatMemberApiRequest(
        long userId,
        Boolean block
) {
}
