package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Docs-shaped request for PATCH /chats/{chatId}.
 */
public record UpdateChatApiRequest(
        Map<String, Object> icon,
        String title,
        Boolean pin,
        @JsonProperty("notify") Boolean notifyValue
) {
}
