package ru.max.botframework.model.request;

import java.util.Objects;

/**
 * Low-level inline keyboard button payload.
 */
public record InlineKeyboardButtonRequest(
        String text,
        String callbackData,
        String url
) {
    public InlineKeyboardButtonRequest {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }

        boolean hasCallback = callbackData != null && !callbackData.isBlank();
        boolean hasUrl = url != null && !url.isBlank();
        if (hasCallback == hasUrl) {
            throw new IllegalArgumentException("Exactly one of callbackData or url must be provided");
        }
    }
}
