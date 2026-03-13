package ru.max.botframework.model.request;

import java.util.Objects;
import ru.max.botframework.model.ChatAction;

/**
 * Request DTO for sending chat action.
 */
public record SendChatActionRequest(ChatAction action) {
    public SendChatActionRequest {
        Objects.requireNonNull(action, "action");
    }
}
