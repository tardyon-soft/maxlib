package ru.max.botframework.message;

import java.util.Objects;
import ru.max.botframework.model.request.InlineKeyboardButtonRequest;

/**
 * Immutable high-level inline keyboard button model.
 */
public record InlineKeyboardButton(
        Kind kind,
        String text,
        String value
) {
    public InlineKeyboardButton {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(value, "value");
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    InlineKeyboardButtonRequest toRequest() {
        return switch (kind) {
            case CALLBACK -> new InlineKeyboardButtonRequest(text, value, null);
            case LINK -> new InlineKeyboardButtonRequest(text, null, value);
        };
    }

    public enum Kind {
        CALLBACK,
        LINK
    }
}
