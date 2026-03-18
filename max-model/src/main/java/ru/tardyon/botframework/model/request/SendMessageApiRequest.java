package ru.tardyon.botframework.model.request;

import java.util.Objects;
import ru.tardyon.botframework.model.MessageId;

/**
 * Docs-shaped request for POST /messages with user/chat recipient selectors.
 */
public record SendMessageApiRequest(
        Long userId,
        Long chatId,
        Boolean disableLinkPreview,
        NewMessageBody body,
        Boolean sendNotification,
        MessageId replyToMessageId
) {
    public SendMessageApiRequest {
        Objects.requireNonNull(body, "body");
        if (userId == null && chatId == null) {
            throw new IllegalArgumentException("Either userId or chatId must be provided");
        }
    }
}
