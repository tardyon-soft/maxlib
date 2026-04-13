package ru.tardyon.botframework.model.request;

import java.util.Objects;

/**
 * Low-level inline keyboard button payload.
 */
public record InlineKeyboardButtonRequest(
        String text,
        String callbackData,
        String clipboardPayload,
        String url,
        Boolean requestContact,
        Boolean requestGeoLocation,
        String openApp,
        String message
) {
    public InlineKeyboardButtonRequest {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }

        boolean hasCallback = callbackData != null && !callbackData.isBlank();
        boolean hasClipboard = clipboardPayload != null && !clipboardPayload.isBlank();
        boolean hasUrl = url != null && !url.isBlank();
        boolean hasRequestContact = Boolean.TRUE.equals(requestContact);
        boolean hasRequestGeoLocation = Boolean.TRUE.equals(requestGeoLocation);
        boolean hasOpenApp = openApp != null && !openApp.isBlank();
        boolean hasMessage = message != null && !message.isBlank();

        int actionCount = 0;
        actionCount += hasCallback ? 1 : 0;
        actionCount += hasClipboard ? 1 : 0;
        actionCount += hasUrl ? 1 : 0;
        actionCount += hasRequestContact ? 1 : 0;
        actionCount += hasRequestGeoLocation ? 1 : 0;
        actionCount += hasOpenApp ? 1 : 0;
        actionCount += hasMessage ? 1 : 0;
        if (actionCount != 1) {
            throw new IllegalArgumentException("Exactly one button action must be provided");
        }
    }
}
