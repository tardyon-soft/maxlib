package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Docs-shaped request for PATCH /chats/{chatId}.
 */
public record UpdateChatApiRequest(
        PhotoAttachmentRequestPayload icon,
        String title,
        String pin,
        @JsonProperty("notify") Boolean notifyValue
) {
}
