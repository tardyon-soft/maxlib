package ru.tardyon.botframework.model.request;

import java.util.Objects;
import ru.tardyon.botframework.model.ChatAction;

/**
 * Request DTO for sending chat action.
 */
public record SendChatActionRequest(ChatAction action) {
    public SendChatActionRequest {
        Objects.requireNonNull(action, "action");
    }
}
