package ru.tardyon.botframework.message;

import java.util.Objects;
import ru.tardyon.botframework.model.request.InlineKeyboardButtonRequest;

/**
 * Immutable high-level inline keyboard button model.
 */
public record InlineKeyboardButton(
        Kind kind,
        String text,
        String callbackData,
        String clipboardPayload,
        String url,
        String openApp,
        String message
) {
    public InlineKeyboardButton {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }

        switch (kind) {
            case CALLBACK -> requireNonBlank(callbackData, "callbackData");
            case CLIPBOARD -> requireNonBlank(clipboardPayload, "clipboardPayload");
            case LINK -> {
                requireNonBlank(url, "url");
                if (url.length() > InlineKeyboardConstraints.MAX_LINK_URL_LENGTH) {
                    throw new IllegalArgumentException(
                            "url length must be <= %d".formatted(InlineKeyboardConstraints.MAX_LINK_URL_LENGTH)
                    );
                }
            }
            case OPEN_APP -> requireNonBlank(openApp, "openApp");
            case MESSAGE -> requireNonBlank(message, "message");
            case REQUEST_CONTACT, REQUEST_GEO_LOCATION -> {
                // no extra payload required
            }
        }
    }

    InlineKeyboardButtonRequest toRequest() {
        return switch (kind) {
            case CALLBACK -> new InlineKeyboardButtonRequest(text, callbackData, null, null, null, null, null, null);
            case CLIPBOARD -> new InlineKeyboardButtonRequest(text, null, clipboardPayload, null, null, null, null, null);
            case LINK -> new InlineKeyboardButtonRequest(text, null, null, url, null, null, null, null);
            case REQUEST_CONTACT -> new InlineKeyboardButtonRequest(text, null, null, null, true, null, null, null);
            case REQUEST_GEO_LOCATION -> new InlineKeyboardButtonRequest(text, null, null, null, null, true, null, null);
            case OPEN_APP -> new InlineKeyboardButtonRequest(text, null, null, null, null, null, openApp, null);
            case MESSAGE -> new InlineKeyboardButtonRequest(text, null, null, null, null, null, null, message);
        };
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    public enum Kind {
        CALLBACK,
        CLIPBOARD,
        LINK,
        REQUEST_CONTACT,
        REQUEST_GEO_LOCATION,
        OPEN_APP,
        MESSAGE
    }
}
